package com.agricultural.assistplatform.config;

import com.agricultural.assistplatform.annotation.RequireLoginType;
import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.exception.BusinessException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginTypeInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequireLoginType methodAnnotation = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), RequireLoginType.class);
        RequireLoginType classAnnotation = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequireLoginType.class);
        String requiredType = methodAnnotation != null ? methodAnnotation.value() : classAnnotation != null ? classAnnotation.value() : null;
        if (!StringUtils.hasText(requiredType)) {
            return true;
        }

        Long userId = LoginContext.getUserId();
        String loginType = LoginContext.getLoginType();
        if (userId == null || !requiredType.equalsIgnoreCase(String.valueOf(loginType))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先以" + displayName(requiredType) + "身份登录");
        }
        return true;
    }

    private String displayName(String requiredType) {
        return switch (requiredType.toLowerCase()) {
            case "admin" -> "管理员";
            case "merchant" -> "商家";
            default -> "用户";
        };
    }
}
