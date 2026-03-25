package com.agricultural.assistplatform.config;

import com.agricultural.assistplatform.service.common.OrderCommunicationService;
import com.agricultural.assistplatform.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final OrderCommunicationService orderCommunicationService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        MultiValueMap<String, String> query = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        String token = query.getFirst("token");
        String orderNo = query.getFirst("orderNo");

        if (!StringUtils.hasText(token) || !StringUtils.hasText(orderNo)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            Long userId = jwtUtil.getUserId(token);
            String loginType = jwtUtil.getType(token);
            if (userId == null || !StringUtils.hasText(loginType)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            if (!orderCommunicationService.canAccessOrder(orderNo, userId, loginType)) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }
            attributes.put("userId", userId);
            attributes.put("loginType", loginType);
            attributes.put("orderNo", orderNo);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}

