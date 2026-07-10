# Chat System

基于 Spring Boot + WebSocket + MyBatis-Plus + Vue 3 的实时聊天系统。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.5.16 |
| 认证授权 | Spring Security + JWT (jjwt) | — |
| ORM | MyBatis-Plus | — |
| 实时通信 | Spring WebSocket | — |
| 数据库 | PostgreSQL | 16 |
| 缓存 | Redis (Lettuce) | 7 |
| 前端 | Vue 3 + TypeScript + Vite | — |
| UI 框架 | Element Plus | — |
| 状态管理 | Pinia | — |
| 语言 | Java 21 | — |

## 项目结构

```
chat-system/
├── chat-ui/                     # 前端项目 (Vue 3)
├── sql/                         # 数据库迁移脚本
├── skills/                      # 开发规范定义
├── src/main/java/com/zjj/chatsystem/
│   ├── common/                  # 公共模块 (Result/ErrorCode/异常)
│   ├── config/                  # 配置类 (Security/Redis/WebSocket/MyBatis)
│   ├── controller/              # 控制器 (Auth/Chat/Friend)
│   ├── domain/
│   │   ├── dto/                 # 请求/响应 DTO
│   │   ├── entity/              # 数据库实体
│   │   └── query/               # 分页查询参数
│   ├── mapper/                  # MyBatis Mapper
│   ├── service/                 # 业务逻辑层
│   ├── websocket/               # WebSocket 处理器
│   └── utils/                   # 工具类 (JWT/Redis)
└── pom.xml
```

## 数据库表

当前数据库 `chat_system` 包含以下 4 张表：

### 1. `users` — 用户表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | `bigint` | PK | 用户 ID（雪花算法） |
| username | `varchar(50)` | UNIQUE, NOT NULL | 用户名 |
| password | `varchar(255)` | NOT NULL | 密码（BCrypt 加密） |
| nickname | `varchar(50)` | | 昵称 |
| avatar | `varchar(255)` | | 头像 URL |
| email | `varchar(100)` | | 邮箱 |
| status | `smallint` | DEFAULT 0 | 状态: 0-离线, 1-在线 |
| deleted | `smallint` | DEFAULT 0 | 逻辑删除: 0-未删, 1-已删 |
| created_at | `timestamp` | DEFAULT NOW() | 创建时间 |
| updated_at | `timestamp` | DEFAULT NOW() | 更新时间 |

**索引**: `users_pkey` (id), `users_username_key` (username), `idx_users_status`, `idx_users_username`

### 2. `chat_messages` — 聊天消息表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | `bigint` | PK | 消息 ID（雪花算法） |
| sender_id | `bigint` | FK → users(id) | 发送者 ID |
| receiver_id | `bigint` | | 接收者 ID（私聊），群聊时为 NULL |
| content | `text` | | 消息内容 |
| message_type | `varchar(20)` | DEFAULT 'TEXT' | 类型: TEXT, IMAGE, FILE, SYSTEM |
| status | `varchar(20)` | DEFAULT 'SENT' | 状态: SENT, DELIVERED, READ |
| group_id | `bigint` | | 群组 ID（群聊），私聊时为 NULL |
| created_at | `timestamp` | DEFAULT NOW() | 发送时间 |

**外键**: `fk_msg_sender` → users(id) ON DELETE CASCADE
**索引**: `idx_msg_sender`, `idx_msg_receiver`, `idx_msg_chat`, `idx_msg_group`, `idx_msg_time`

### 3. `user_friend` — 好友关系表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | `bigint` | PK (自增序列) | 关系 ID |
| user_id | `bigint` | FK → users(id) ON DELETE CASCADE | 用户 ID |
| friend_id | `bigint` | FK → users(id) ON DELETE CASCADE | 好友 ID |
| deleted | `bigint` | NOT NULL DEFAULT 0 | 逻辑删除: 0-未删, 1-已删 |
| created_at | `timestamp` | DEFAULT NOW() | 添加时间 |

**约束**: `chk_user_friend` — 保证 `user_id <> friend_id`
**唯一索引**: `uq_user_friend` (user_id, friend_id) — 不留重复好友关系
**索引**: `idx_friend_user`, `idx_friend_friend`

> 好友关系双向存储（A→B 和 B→A 各一条记录），保证双向快速查询。

### 4. `friend_requests` — 好友请求表

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | `bigint` | PK | 请求 ID |
| from_user_id | `bigint` | FK → users(id) | 申请人 ID |
| to_user_id | `bigint` | FK → users(id) | 接收人 ID |
| status | `integer` | NOT NULL DEFAULT 0 | 状态: 0-待处理, 1-已同意, 2-已拒绝 |
| deleted | `integer` | NOT NULL DEFAULT 0 | 逻辑删除: 0-正常, 1-删除 |
| created_at | `timestamp` | NOT NULL DEFAULT NOW() | 创建时间 |
| updated_at | `timestamp` | NOT NULL DEFAULT NOW() | 更新时间 |

**外键**: `fk_friend_requests_from_user` → users(id), `fk_friend_requests_to_user` → users(id)
**索引**: `idx_friend_requests_to_user_status` (to_user_id, status, deleted)

### 关系图

```
users (1) ────────── (N) user_friend (N) ────────── (1) users
  │                                                        │
  │ (1)                                                    │ (1)
  │                                                        │
  └────────── (N) chat_messages (N) ──────────────────────┘
                        │
                        │ (sender_id) → users(id)
                        │ (receiver_id) → users(id)

users (1) ──── (N) friend_requests (N) ──── (1) users
  │ (from)                                  │ (to)
```

## API 接口

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录 |

### 聊天

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/chat/users` | 获取当前用户信息 |
| GET | `/api/chat/online-users` | 获取在线用户列表 |

### 好友

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/friend/request` | 发送好友请求 |
| POST | `/api/friend/accept/{id}` | 接受好友请求 |
| POST | `/api/friend/reject/{id}` | 拒绝好友请求 |
| GET | `/api/friend/requests` | 收到的待处理请求 |
| GET | `/api/friend/requests/sent` | 已发送的请求 |
| GET | `/api/friend/list` | 好友列表 |
| POST | `/api/friend/search` | 搜索用户 |
| DELETE | `/api/friend/remove/{userId}` | 删除好友 |

### WebSocket

| 端点 | 说明 |
|------|------|
| `/ws/chat?token={jwt}` | 聊天 WebSocket 连接 |

## 配置说明

参考 `.env.example`，需要配置：
- PostgreSQL 数据库连接
- Redis 连接
- JWT secret

## 构建与运行

```bash
# 后端
./mvnw clean package
./mvnw spring-boot:run

# 前端
cd chat-ui
npm install
npm run dev
```

## 开发规范

所有开发规范详见 `skills/` 目录：
- `enterprise-arch.md` — 企业级项目架构规范
- `db-postgresql.md` — PostgreSQL 数据库开发规范
- `redis-standards.md` — Redis 使用规范
- `vue-frontend.md` — Vue 3 前端开发规范
- `ui-design.md` — UI 美术设计规范
