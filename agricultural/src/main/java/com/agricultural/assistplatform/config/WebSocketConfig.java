package com.agricultural.assistplatform.config;

import com.agricultural.assistplatform.websocket.OrderChatWebSocketHandler;
import com.agricultural.assistplatform.websocket.RealtimeWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderChatWebSocketHandler orderChatWebSocketHandler;
    private final WebSocketAuthHandshakeInterceptor webSocketAuthHandshakeInterceptor;
    private final RealtimeWebSocketHandler realtimeWebSocketHandler;
    private final RealtimeWebSocketAuthHandshakeInterceptor realtimeWebSocketAuthHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(orderChatWebSocketHandler, "/ws/order-chat")
                .addInterceptors(webSocketAuthHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
        registry.addHandler(realtimeWebSocketHandler, "/ws/realtime")
                .addInterceptors(realtimeWebSocketAuthHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
