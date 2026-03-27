package com.agricultural.assistplatform.config;

import com.agricultural.assistplatform.annotation.AdminPermission;
import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.service.admin.AdminPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class AdminPermissionInterceptor implements HandlerInterceptor {

    private final AdminPermissionService adminPermissionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        AdminPermission methodPermission = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), AdminPermission.class);
        AdminPermission classPermission = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), AdminPermission.class);
        String permissionCode = methodPermission != null ? methodPermission.value() : classPermission != null ? classPermission.value() : null;
        adminPermissionService.assertAdminPermission(LoginContext.getUserId(), LoginContext.getLoginType(), permissionCode);
        return true;
    }
}
