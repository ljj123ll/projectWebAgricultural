package com.agricultural.assistplatform.service.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeWebSocketService {

    private final ObjectMapper objectMapper;

    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Map<Long, Set<WebSocketSession>> merchantSessions = new ConcurrentHashMap<>();
    private final Map<Long, Set<WebSocketSession>> adminSessions = new ConcurrentHashMap<>();

    public void register(String loginType, Long userId, WebSocketSession session) {
        if (userId == null || session == null || !StringUtils.hasText(loginType)) return;
        resolveSessions(loginType).computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(String loginType, Long userId, WebSocketSession session) {
        if (userId == null || session == null || !StringUtils.hasText(loginType)) return;
        Map<Long, Set<WebSocketSession>> sessionsMap = resolveSessions(loginType);
        Set<WebSocketSession> sessions = sessionsMap.get(userId);
        if (sessions == null) return;
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsMap.remove(userId);
        }
    }

    public void sendConnected(WebSocketSession session, String loginType, Long userId) {
        send(session, Map.of(
                "type", "connected",
                "role", loginType == null ? "" : loginType,
                "userId", userId == null ? 0L : userId,
                "time", LocalDateTime.now().toString()
        ));
    }

    public void publishUserRefresh(Long userId, String reason, String refNo) {
        publish("user", userId, reason, refNo);
    }

    public void publishUserRefreshToAll(String reason, String refNo) {
        userSessions.keySet().forEach(userId -> publishUserRefresh(userId, reason, refNo));
    }

    public void publishMerchantRefresh(Long merchantId, String reason, String refNo) {
        publish("merchant", merchantId, reason, refNo);
    }

    public void publishAdminRefresh(Long adminId, String reason, String refNo) {
        publish("admin", adminId, reason, refNo);
    }

    public void publishAdminRefreshToAll(String reason, String refNo) {
        adminSessions.keySet().forEach(adminId -> publishAdminRefresh(adminId, reason, refNo));
    }

    private void publish(String loginType, Long userId, String reason, String refNo) {
        if (userId == null || !StringUtils.hasText(loginType)) return;
        Set<WebSocketSession> sessions = resolveSessions(loginType).get(userId);
        if (sessions == null || sessions.isEmpty()) return;
        Map<String, Object> payload = new ConcurrentHashMap<>();
        payload.put("type", "refresh");
        payload.put("data", Map.of(
                "reason", reason == null ? "" : reason,
                "refNo", refNo == null ? "" : refNo,
                "timestamp", System.currentTimeMillis()
        ));
        sessions.forEach(session -> send(session, payload));
    }

    private Map<Long, Set<WebSocketSession>> resolveSessions(String loginType) {
        return switch (String.valueOf(loginType)) {
            case "merchant" -> merchantSessions;
            case "admin" -> adminSessions;
            default -> userSessions;
        };
    }

    private void send(WebSocketSession session, Map<String, Object> payload) {
        if (session == null || !session.isOpen()) return;
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (IOException ex) {
            log.warn("Realtime websocket send failed, session={}", session.getId());
        }
    }
}
