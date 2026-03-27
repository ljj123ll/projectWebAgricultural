package com.agricultural.assistplatform.websocket;

import com.agricultural.assistplatform.service.common.RealtimeWebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private final RealtimeWebSocketService realtimeWebSocketService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getLongAttr(session, "userId");
        String loginType = stringAttr(session, "loginType");
        if (userId == null || !StringUtils.hasText(loginType)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("登录状态无效"));
            return;
        }
        realtimeWebSocketService.register(loginType, userId, session);
        realtimeWebSocketService.sendConnected(session, loginType, userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 通知通道仅服务端推送，不处理客户端消息
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
        realtimeWebSocketService.unregister(stringAttr(session, "loginType"), getLongAttr(session, "userId"), session);
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
