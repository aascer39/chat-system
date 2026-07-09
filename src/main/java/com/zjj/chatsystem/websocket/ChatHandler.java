package com.zjj.chatsystem.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjj.chatsystem.domain.entity.ChatMessage;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatHandler.class);
    private static final Map<String, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    public ChatHandler(ObjectMapper objectMapper, JwtUtil jwtUtil) {
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = extractToken(session);
        if (token == null || !jwtUtil.validateToken(token)) {
            closeSession(session);
            return;
        }

        String username = jwtUtil.getUsernameFromToken(token);
        onlineUsers.put(username, session);
        session.getAttributes().put("username", username);

        log.info("用户 {} 已连接，当前在线人数: {}", username, onlineUsers.size());
        broadcastOnlineCount();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String username = (String) session.getAttributes().get("username");
            ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            chatMessage.setSenderId(getUserIdByUsername(username));
            chatMessage.setStatus("SENT");
            chatMessage.setCreatedAt(LocalDateTime.now());

            // 发送给指定用户
            String receiver = getUsernameById(chatMessage.getReceiverId());
            WebSocketSession receiverSession = onlineUsers.get(receiver);
            if (receiverSession != null && receiverSession.isOpen()) {
                chatMessage.setStatus("DELIVERED");
                receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(chatMessage)));
            }

            // 回复发送者确认
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
        String countMessage = "{\"type\":\"ONLINE_COUNT\",\"count\":" + onlineUsers.size() + "}";
        TextMessage message = new TextMessage(countMessage);
        onlineUsers.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (IOException e) {
                log.error("广播在线人数失败", e);
            }
        });
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("token=")) {
            return query.substring(6);
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

    // 临时方法，后续从 UserService 获取
    private Long getUserIdByUsername(String username) {
        return (long) username.hashCode();
    }

    private String getUsernameById(Long userId) {
        return String.valueOf(userId);
    }
}
