package com.agricultural.assistplatform.controller.user;

import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.service.common.UserRealtimeEventService;
import com.agricultural.assistplatform.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "用户端-实时通知")
@RestController
@RequestMapping("/user/realtime")
@RequiredArgsConstructor
public class UserRealtimeController {

    private final JwtUtil jwtUtil;
    private final UserRealtimeEventService userRealtimeEventService;

    @Operation(summary = "订阅用户实时通知")
    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam(required = false) String token) {
        Long userId = parseUserId(token);
        return userRealtimeEventService.subscribe(userId);
    }

    private Long parseUserId(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        try {
            Long userId = jwtUtil.getUserId(token);
            String type = jwtUtil.getType(token);
            if (userId == null || !"user".equals(type)) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
            }
            return userId;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已过期");
        }
    }
}
