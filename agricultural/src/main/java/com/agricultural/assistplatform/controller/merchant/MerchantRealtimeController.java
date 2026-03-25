package com.agricultural.assistplatform.controller.merchant;

import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.service.common.MerchantRealtimeEventService;
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

@Tag(name = "商家端-实时通知")
@RestController
@RequestMapping("/merchant/realtime")
@RequiredArgsConstructor
public class MerchantRealtimeController {

    private final JwtUtil jwtUtil;
    private final MerchantRealtimeEventService merchantRealtimeEventService;

    @Operation(summary = "订阅商家实时通知")
    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam(required = false) String token) {
        Long merchantId = parseMerchantId(token);
        return merchantRealtimeEventService.subscribe(merchantId);
    }

    private Long parseMerchantId(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        try {
            Long userId = jwtUtil.getUserId(token);
            String type = jwtUtil.getType(token);
            if (userId == null || !"merchant".equals(type)) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
            }
            return userId;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已过期");
        }
    }
}
