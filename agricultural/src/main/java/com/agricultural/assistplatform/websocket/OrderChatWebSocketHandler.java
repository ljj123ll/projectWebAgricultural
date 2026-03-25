package com.agricultural.assistplatform.websocket;

import com.agricultural.assistplatform.entity.OrderCommunication;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.service.common.OrderCommunicationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final OrderCommunicationService orderCommunicationService;

    private final ConcurrentHashMap<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionOrderMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String orderNo = String.valueOf(session.getAttributes().get("orderNo"));
        if (!StringUtils.hasText(orderNo) || "null".equals(orderNo)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("缺少订单号"));
            return;
        }
        roomSessions.computeIfAbsent(orderNo, key -> ConcurrentHashMap.newKeySet()).add(session);
        sessionOrderMap.put(session.getId(), orderNo);
        sendJson(session, Map.of(
                "type", "connected",
                "orderNo", orderNo,
                "time", LocalDateTime.now().toString()
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String orderNo = sessionOrderMap.get(session.getId());
        Long userId = getLongAttr(session, "userId");
        String loginType = stringAttr(session, "loginType");
        if (!StringUtils.hasText(orderNo) || userId == null || !StringUtils.hasText(loginType)) {
            sendError(session, "连接状态异常，请刷新后重试");
            return;
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), new TypeReference<>() {});
            String content = payload != null && payload.get("content") != null
                    ? String.valueOf(payload.get("content"))
                    : null;
            Integer messageType = parseInt(payload == null ? null : payload.get("messageType"));
            String mediaUrl = payload != null && payload.get("mediaUrl") != null
                    ? String.valueOf(payload.get("mediaUrl"))
                    : null;
            String mediaName = payload != null && payload.get("mediaName") != null
                    ? String.valueOf(payload.get("mediaName"))
                    : null;
            OrderCommunication saved = orderCommunicationService.send(orderNo, userId, loginType, content, messageType, mediaUrl, mediaName);
            broadcast(orderNo, Map.of(
                    "type", "message",
                    "data", toPayload(saved)
            ));
        } catch (BusinessException ex) {
            sendError(session, ex.getMessage());
        } catch (Exception ex) {
            log.error("Order chat message handle failed", ex);
            sendError(session, "消息发送失败，请稍后再试");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        cleanup(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cleanup(session);
    }

    private void cleanup(WebSocketSession session) {
        String orderNo = sessionOrderMap.remove(session.getId());
        if (!StringUtils.hasText(orderNo)) return;
        Set<WebSocketSession> sessions = roomSessions.get(orderNo);
        if (sessions == null) return;
        sessions.remove(session);
        if (sessions.isEmpty()) {
            roomSessions.remove(orderNo);
        }
    }

    private void broadcast(String orderNo, Map<String, Object> payload) {
        Set<WebSocketSession> sessions = roomSessions.get(orderNo);
        if (sessions == null || sessions.isEmpty()) return;
        String text;
        try {
            text = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Order chat broadcast serialize failed", e);
            return;
        }
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) continue;
            try {
                session.sendMessage(new TextMessage(text));
            } catch (IOException ex) {
                log.warn("Order chat send failed, session={}", session.getId());
            }
        }
    }

    private void sendError(WebSocketSession session, String message) {
        sendJson(session, Map.of(
                "type", "error",
                "message", message
        ));
    }

    private void sendJson(WebSocketSession session, Map<String, Object> payload) {
        if (!session.isOpen()) return;
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception ex) {
            log.warn("Order chat send json failed, session={}", session.getId());
        }
    }

    private Map<String, Object> toPayload(OrderCommunication message) {
        return Map.of(
                "id", message.getId() == null ? 0L : message.getId(),
                "orderNo", message.getOrderNo() == null ? "" : message.getOrderNo(),
                "senderType", message.getSenderType() == null ? 0 : message.getSenderType(),
                "senderId", message.getSenderId() == null ? 0L : message.getSenderId(),
                "messageType", message.getMessageType() == null ? 1 : message.getMessageType(),
                "content", message.getContent() == null ? "" : message.getContent(),
                "mediaUrl", message.getMediaUrl() == null ? "" : message.getMediaUrl(),
                "mediaName", message.getMediaName() == null ? "" : message.getMediaName(),
                "createTime", message.getCreateTime() == null ? LocalDateTime.now().toString() : message.getCreateTime().toString()
        );
    }

    private Integer parseInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private Long getLongAttr(WebSocketSession session, String key) {
        Object value = session.getAttributes().get(key);
        if (value instanceof Long longValue) return longValue;
        if (value == null) return null;
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private String stringAttr(WebSocketSession session, String key) {
        Object value = session.getAttributes().get(key);
        return value == null ? null : String.valueOf(value);
    }
}
