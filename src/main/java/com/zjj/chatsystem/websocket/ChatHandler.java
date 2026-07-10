package com.zjj.chatsystem.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjj.chatsystem.domain.entity.ChatMessage;
import com.zjj.chatsystem.domain.entity.GroupMember;
import com.zjj.chatsystem.domain.entity.User;
import com.zjj.chatsystem.mapper.UserMapper;
import com.zjj.chatsystem.service.ChatMessageService;
import com.zjj.chatsystem.service.GroupService;
import com.zjj.chatsystem.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatHandler.class);
    private static final Map<String, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final ChatMessageService chatMessageService;
    private final GroupService groupService;

    public ChatHandler(ObjectMapper objectMapper, JwtUtil jwtUtil, UserMapper userMapper,
                       ChatMessageService chatMessageService, GroupService groupService) {
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.chatMessageService = chatMessageService;
        this.groupService = groupService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = extractToken(session);
        if (token == null || !jwtUtil.validateToken(token)) {
            closeSession(session);
            return;
        }

        String username = jwtUtil.getUsernameFromToken(token);

        // 获取用户 ID
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

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());

            // ── 系统消息 ──
            JsonNode typeNode = root.get("type");
            if (typeNode != null) {
                String type = typeNode.asText();
                switch (type) {
                    case "PING" -> {
                        session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
                        return;
                    }
                    case "READ" -> {
                        // 已读回执
                        Long messageId = root.get("messageId").asLong();
                        chatMessageService.updateStatus(messageId, "READ");

                        // 通知原发送方
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
                    default -> {
                        return;
                    }
                }
            }

            // ── 聊天消息 ──
            String username = (String) session.getAttributes().get("username");
            ChatMessage chatMessage = objectMapper.treeToValue(root, ChatMessage.class);
            chatMessage.setSenderId(getUserIdByUsername(username));
            chatMessage.setStatus("SENT");
            chatMessage.setCreatedAt(LocalDateTime.now());

            // 写入数据库（同步，之后 chatMessage.getId() 可用）
            chatMessageService.saveMessage(chatMessage);

            if (chatMessage.getGroupId() != null) {
                // ── 群消息：推送给所有在线群成员（除发送方） ──
                User sender = userMapper.selectById(chatMessage.getSenderId());
                chatMessage.setSenderName(sender != null ? sender.getNickname() : sender.getUsername());
                chatMessage.setStatus("DELIVERED");
                chatMessageService.updateStatus(chatMessage.getId(), "DELIVERED");
                String msgJson = objectMapper.writeValueAsString(chatMessage);

                List<GroupMember> members = groupService.getMembers(chatMessage.getGroupId());
                for (GroupMember member : members) {
                    if (member.getUserId().equals(chatMessage.getSenderId())) continue;
                    String memberName = getUsernameById(member.getUserId());
                    WebSocketSession memberSession = onlineUsers.get(memberName);
                    if (memberSession != null && memberSession.isOpen()) {
                        memberSession.sendMessage(new TextMessage(msgJson));
                    }
                }
            } else {
                // ── 单聊：推送给接收方 ──
                String receiver = getUsernameById(chatMessage.getReceiverId());
                WebSocketSession receiverSession = onlineUsers.get(receiver);
                if (receiverSession != null && receiverSession.isOpen()) {
                    chatMessage.setStatus("DELIVERED");
                    chatMessageService.updateStatus(chatMessage.getId(), "DELIVERED");
                    receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(chatMessage)));
                }
            }

            // 回复发送者确认（包含最新状态）
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(chatMessage)));

        } catch (Exception e) {
            log.error("处理消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            onlineUsers.remove(username);
            log.info("用户 {} 已断开连接，当前在线人数: {}", username, onlineUsers.size());
            broadcastOnlineCount();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("传输错误: {}", exception.getMessage());
    }

    private void broadcastOnlineCount() {
        try {
            List<Long> onlineUserIds = new ArrayList<>();
            for (WebSocketSession s : onlineUsers.values()) {
                Long userId = (Long) s.getAttributes().get("userId");
                if (userId != null) {
                    onlineUserIds.add(userId);
                }
            }

            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "ONLINE_COUNT",
                    "count", onlineUserIds.size(),
                    "onlineUserIds", onlineUserIds
            ));
            TextMessage message = new TextMessage(json);

            for (WebSocketSession s : onlineUsers.values()) {
                if (s.isOpen()) {
                    s.sendMessage(message);
                }
            }
        } catch (Exception e) {
            log.error("广播在线用户列表失败", e);
        }
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2 && "token".equals(pair[0])) {
                    return pair[1];
                }
            }
        }
        return null;
    }

    private void closeSession(WebSocketSession session) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException e) {
            log.error("关闭连接失败", e);
        }
    }

    private Long getUserIdByUsername(String username) {
        User user = userMapper.findByUsername(username).orElse(null);
        return user != null ? user.getId() : (long) username.hashCode();
    }

    private String getUsernameById(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getUsername() : String.valueOf(userId);
    }
}
