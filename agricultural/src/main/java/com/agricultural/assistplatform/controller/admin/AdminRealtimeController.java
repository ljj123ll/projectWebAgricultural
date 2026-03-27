package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.service.common.AdminRealtimeEventService;
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

@Tag(name = "管理员端-实时通知")
@RestController
@RequestMapping("/admin/realtime")
@RequiredArgsConstructor
public class AdminRealtimeController {

    private final JwtUtil jwtUtil;
    private final AdminRealtimeEventService adminRealtimeEventService;

    @Operation(summary = "订阅管理员实时通知")
    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam(required = false) String token) {
        Long adminId = parseAdminId(token);
        return adminRealtimeEventService.subscribe(adminId);
    }

    private Long parseAdminId(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        try {
            Long userId = jwtUtil.getUserId(token);
            String type = jwtUtil.getType(token);
            if (userId == null || !"admin".equals(type)) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
            }
            return userId;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已过期");
        }
    }
}
