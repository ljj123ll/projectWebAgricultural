package com.agricultural.assistplatform.config;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 从请求头 Authorization: Bearer {token} 解析并设置当前登录用户/商家/管理员
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String auth = request.getHeader("Authorization");
            if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                Long userId = jwtUtil.getUserId(token);
                String type = jwtUtil.getType(token);
                if (userId != null) {
                    LoginContext.setUserId(userId);
                    LoginContext.setLoginType(type == null ? "user" : type);
                }
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException | SignatureException e) {
            filterChain.doFilter(request, response);
        } finally {
            LoginContext.clear();
        }
    }
}
