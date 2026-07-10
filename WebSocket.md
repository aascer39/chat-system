# WebSocket 实时通信机制

## 架构总览

```
┌─────────────────────────────────────────────────┐
│                    客户端                         │
│  new WebSocket("ws://host/ws/chat?token=xxx")    │
└────────────────────┬────────────────────────────┘
                     │ upgrade 握手 (101 Switching Protocols)
                     ▼
┌─────────────────────────────────────────────────┐
│  WebSocketConfig            (/ws/chat 端点)      │
│  registry.addHandler(chatHandler, "/ws/chat")    │
└────────────────────┬────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────┐
│  ChatHandler (extends TextWebSocketHandler)      │
│                                                  │
│  afterConnectionEstablished  → 鉴权 + 存会话      │
│  handleTextMessage           → 处理消息           │
│  afterConnectionClosed       → 清理 + 广播        │
│  handleTransportError        → 异常处理           │
└─────────────────────────────────────────────────┘
```

本项目同时使用 **HTTP REST** 和 **WebSocket** 两种通信方式：

| | HTTP REST | WebSocket |
|--|-----------|-----------|
| 协议模式 | 请求-响应 | 全双工 |
| 连接 | 每次请求新建 | 长连接（维持到断线） |
| 实时性 | 需要轮询或 SSE | 服务端可主动推送 |
| 状态 | 无状态（靠 JWT 鉴权） | 有状态（session 属性） |
| 适用场景 | 登录注册、好友管理、历史查询 | 实时消息收发、在线状态推送、已读回执 |

---

## 1. 端点注册 — WebSocketConfig

**文件**: `config/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatHandler chatHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler, "/ws/chat")
                .setAllowedOrigins("*");
    }
}
```

- `@EnableWebSocket` — 开启 Spring WebSocket 支持
- `/ws/chat` — WebSocket 端点，完整 URL 为 `ws://host/ws/chat?token=xxx`
- `setAllowedOrigins("*")` — 允许跨域（开发环境）
- `ChatHandler` 作为 handler 注入，处理所有连接/消息/断开事件

### 握手流程

```
客户端                             服务端
  │                                  │
  ├── HTTP Upgrade Request ────────→ │
  │   GET /ws/chat?token=xxx         │
  │   Upgrade: websocket             │
  │   Connection: Upgrade            │
  │   Sec-WebSocket-Key: ...         │
  │                                  │
  │ ←── 101 Switching Protocols ────┤
  │   Upgrade: websocket             │
  │   Connection: Upgrade            │
  │   Sec-WebSocket-Accept: ...      │
  │                                  │
  │ ←── afterConnectionEstablished ──┤  ← 鉴权在此执行
  │      全双工通信建立                │
```

HTTP 请求先到服务端，携带 `Upgrade: websocket` 头。服务端同意后返回 `101`，协议从 HTTP 升级为 WebSocket。之后的通信不再走 HTTP 协议。

---

## 2. 连接鉴权 — afterConnectionEstablished

**文件**: `websocket/ChatHandler.java`

```java
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    String token = extractToken(session);
    if (token == null || !jwtUtil.validateToken(token)) {
        closeSession(session);
        return;
    }

    String username = jwtUtil.getUsernameFromToken(token);
    User user = userMapper.findByUsername(username).orElse(null);
    if (user == null) {
        closeSession(session);
        return;
    }

    onlineUsers.put(username, session);
    session.getAttributes().put("username", username);
    session.getAttributes().put("userId", user.getId());

    log.info("用户 {} 已连接，当前在线人数: {}", username, onlineUsers.size());
    broadcastOnlineCount();
}
```

### Token 传递

WebSocket 握手时无法自定义 HTTP Header（浏览器 `WebSocket` API 的限制），所以 JWT token 通过 URL query 传递：

```javascript
// 前端
const wsUrl = `ws://${host}/ws/chat?token=${token}`
ws = new WebSocket(wsUrl)
```

服务端从 URI 的 query string 中提取：

```java
private String extractToken(WebSocketSession session) {
    String query = session.getUri().getQuery();
    for (String param : query.split("&")) {
        String[] pair = param.split("=", 2);
        if (pair.length == 2 && "token".equals(pair[0])) {
            return pair[1];
        }
    }
    return null;
}
```

### WebSocketSession

`WebSocketSession` 是连接的核心抽象：

| 方法 | 用途 |
|------|------|
| `sendMessage(TextMessage)` | 向客户端发送消息 |
| `getAttributes()` | 读写会话属性（存 username / userId） |
| `isOpen()` | 判断连接是否存活 |
| `close(CloseStatus)` | 主动关闭连接 |
| `getUri()` | 获取连接 URL（提取 token） |

**注意**: `WebSocketSession` 不是线程安全的，发送消息应串行化。

---

## 3. 消息处理 — handleTextMessage

所有消息按是否有 `type` 字段分为两类：

### 3.1 系统消息（带 `type` 字段）

```java
JsonNode typeNode = root.get("type");
if (typeNode != null) {
    String type = typeNode.asText();
    switch (type) {
        case "PING"  -> { /* 心跳 */ }
        case "READ"  -> { /* 已读回执 */ }
        default      -> { /* 忽略 */ }
    }
}
```

系统消息通过 `type` 字段路由到不同处理逻辑，不进入聊天消息流程。

### 3.2 聊天消息（无 `type` 字段）

```java
// 解析为聊天消息实体
ChatMessage chatMessage = objectMapper.treeToValue(root, ChatMessage.class);
chatMessage.setSenderId(getUserIdByUsername(username));
chatMessage.setStatus("SENT");
chatMessage.setCreatedAt(LocalDateTime.now());

// 写入数据库（同步，之后 ID 可用）
chatMessageService.saveMessage(chatMessage);

// 推送给接收方
String receiver = getUsernameById(chatMessage.getReceiverId());
WebSocketSession receiverSession = onlineUsers.get(receiver);
if (receiverSession != null && receiverSession.isOpen()) {
    chatMessage.setStatus("DELIVERED");
    chatMessageService.updateStatus(chatMessage.getId(), "DELIVERED");
    receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(chatMessage)));
}

// 回显给发送方
session.sendMessage(new TextMessage(objectMapper.writeValueAsString(chatMessage)));
```

### 消息流转图

```
发送方 A                        服务端                        接收方 B
  │                              │                              │
  │── JSON 消息（无 type）──────→ │                              │
  │                              ├── saveMessage (SENT)         │
  │                              ├── updateStatus (DELIVERED)   │
  │                              ├── 推送消息 ────────────────→ │
  │←── 回显确认（DELIVERED）─────┤                              │
  │                              │                              │
  │                              │  ←── {"type":"READ",         │
  │                              │        "messageId":123} ──── │
  │                              │                              │
  │                              ├── updateStatus (READ)        │
  │                              │                              │
  │←── {"type":"READ_RECEIPT",   │                              │
  │      "messageId":123} ───────┤                              │
  │                              │                              │
```

---

## 4. 心跳机制 — PING / PONG

### 作用

检测 WebSocket 连接是否存活，防止中间网络设备（如负载均衡器、防火墙）因长时间无数据而断开连接。

### 实现

**前端**（每 30 秒）：

```javascript
ws.onopen = () => {
    heartbeatTimer = setInterval(() => {
        ws.send(JSON.stringify({ type: 'PING' }))
    }, 30000)
}
```

**后端**：

```java
if ("PING".equals(type)) {
    session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
    return;
}
```

### 断连检测

```
客户端断网（未正常 close）
  ↓
服务端收不到 PING，也收不到 TCP FIN
  ↓
服务端无法立刻感知断连
  ↓
下次 sendMessage 时抛出 IOException → handleTransportError 触发
```

如果只是单纯收不到消息但没有 send，服务端永远不会知道客户端断了。心跳的作用就是定期"确认存活"，如果连续 N 次没收到 PONG，客户端就应该重连。

---

## 5. 在线用户管理

### 数据结构

```java
private static final Map<String, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();
```

Key 为 username，Value 为 WebSocket 会话句柄。`static` 是全局共享状态，`ConcurrentHashMap` 保证线程安全。

### 加入与移除

```java
// 连接时加入
onlineUsers.put(username, session);

// 断开时移除
onlineUsers.remove(username);
```

### 在线列表广播

每次连接/断开都调用 `broadcastOnlineCount()`：

```java
private void broadcastOnlineCount() {
    List<Long> onlineUserIds = new ArrayList<>();
    for (WebSocketSession s : onlineUsers.values()) {
        Long userId = (Long) s.getAttributes().get("userId");
        if (userId != null) onlineUserIds.add(userId);
    }

    String json = objectMapper.writeValueAsString(Map.of(
            "type", "ONLINE_COUNT",
            "count", onlineUserIds.size(),
            "onlineUserIds", onlineUserIds
    ));
    TextMessage message = new TextMessage(json);

    for (WebSocketSession s : onlineUsers.values()) {
        if (s.isOpen()) s.sendMessage(message);
    }
}
```

所有客户端收到后更新在线好友列表的绿点/灰点状态。

---

## 6. 已读回执 — READ / READ_RECEIPT

### 接收方自动发 READ

触发时机（前端）：

1. **新消息到达** — WebSocket 收到对方消息后立即发送
2. **加载历史** — 遍历历史中所有 SENT/DELIVERED 的对方消息，逐条发送

```javascript
function sendReadReceipt(messageId) {
    ws.send(JSON.stringify({ type: 'READ', messageId: String(messageId) }))
}
```

**注意**: 使用 `String(messageId)` 而非 `Number(messageId)`。雪花算法 ID 是 64 位整数，JavaScript 的 `Number` 只能精确表示 53 位整数，强制转换会导致精度丢失（如 `123456789012345678` → `123456789012345680`），后端查不到数据。

### 后端处理

```java
case "READ" -> {
    Long messageId = root.get("messageId").asLong();
    chatMessageService.updateStatus(messageId, "READ");

    ChatMessage readMsg = chatMessageService.getById(messageId);
    if (readMsg != null) {
        String senderName = getUsernameById(readMsg.getSenderId());
        WebSocketSession senderSession = onlineUsers.get(senderName);
        if (senderSession != null && senderSession.isOpen()) {
            String receipt = objectMapper.writeValueAsString(Map.of(
                    "type", "READ_RECEIPT",
                    "messageId", messageId,
                    "senderId", readMsg.getSenderId()
            ));
            senderSession.sendMessage(new TextMessage(receipt));
        }
    }
    return;
}
```

### 前端收到 READ_RECEIPT

```javascript
if (data.type === 'READ_RECEIPT') {
    const msgId = String(data.messageId ?? '')
    const idx = messages.value.findIndex(m => String(m.id) === msgId)
    if (idx >= 0) {
        messages.value[idx] = { ...messages.value[idx], status: 'READ' }
    }
    return
}
```

更新本地消息状态为 READ，发送方看到单勾 → 双勾（蓝色）。

### 消息状态一览

| 状态 | 含义 | 前端显示（自己发的） |
|------|------|--------------------|
| `SENT` | 已保存到数据库 | ✓（半透明白色） |
| `DELIVERED` | 已推送到接收方 | ✓（亮白色） |
| `READ` | 接收方已读 | ✓✓（亮蓝色） |

---

## 7. 连接关闭 — afterConnectionClosed

```java
@Override
public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    String username = (String) session.getAttributes().get("username");
    if (username != null) {
        onlineUsers.remove(username);
        log.info("用户 {} 已断开连接，当前在线人数: {}", username, onlineUsers.size());
        broadcastOnlineCount();
    }
}
```

用户断连 → 从在线列表移除 → 广播在线状态 → 其他客户端更新好友列表。

### 异常处理

```java
@Override
public void handleTransportError(WebSocketSession session, Throwable exception) {
    log.error("传输错误: {}", exception.getMessage());
}
```

传输异常（如网络闪断、超时）时触发，只记录日志，不自动清理会话——连接的 `afterConnectionClosed` 最终会被调用。

---

## 8. 消息类型汇总

### 8.1 WebSocket 消息

| 方向 | type | 载荷 | 作用 |
|------|------|------|------|
| C→S | `PING` | 无 | 心跳请求 |
| S→C | `PONG` | 无 | 心跳响应 |
| C→S | `READ` | `messageId` | 已读回执 |
| S→C | `READ_RECEIPT` | `messageId`, `senderId` | 通知发送方已读 |
| S→C | `ONLINE_COUNT` | `count`, `onlineUserIds` | 在线人数/列表 |
| C→S | (无 type) | 完整 `ChatMessage` JSON | 聊天消息 |
| S→C | (无 type) | 完整 `ChatMessage` JSON | 推送/回显 |

### 8.2 系统消息（无 type 时自动忽略）

```javascript
// 前端 WS 消息分流伪代码
onmessage = (event) => {
    const data = JSON.parse(event.data)

    if (data.type === 'ONLINE_COUNT')  // 在线列表更新
    if (data.type === 'PONG')          // 心跳响应，忽略
    if (data.type === 'READ_RECEIPT')  // 消息已读通知
    if (!data.type)                    // 聊天消息
}
```

---

## 9. WebSocket vs REST 的分工

```
             ┌──────────────────┐
             │    客户端         │
             ├──────────────────┤
             │ REST API         │  ← 登录注册、好友管理、历史记录
             │ WebSocket        │  ← 实时消息、在线状态、已读回执
             └──────────────────┘
```

| 场景 | 使用方式 | 原因 |
|------|---------|------|
| 用户注册/登录 | HTTP POST | 一次性的、无需实时 |
| 好友搜索/添加/删除 | HTTP GET/POST/DELETE | CRUD 操作 |
| 聊天历史加载 | HTTP GET | 分页查询，需要 REST 的缓存和幂等性 |
| 消息收发 | **WebSocket** | 需要服务端主动推送 |
| 在线状态 | **WebSocket** | 状态变化需要实时通知所有客户端 |
| 已读回执 | **WebSocket** | 需要立即通知发送方，不需持久化 |

---

## 10. 集群扩展思考

当前 `onlineUsers` 是单机内存 Map，**无法水平扩展**。如果部署多个实例：

```
用户 A → 实例 1
用户 B → 实例 2

A 发消息给 B → 实例 1 查 onlineUsers 找不到 B → 丢消息 ❌
```

解决方案：使用 Redis 替代本地 Map。

```
用户 A → 实例 1 → Redis Pub/Sub ← 实例 2 ← 用户 B

A 发消息 → 实例 1 存 DB
         → 实例 1 发 Redis Pub 消息
         → 所有实例收到 Redis Sub
         → 实例 2 查本地 Map 找到 B → 推送
```

这也是项目中 `RedisConfig` 和 `RedisService` 存在的意义——为集群化做准备。
