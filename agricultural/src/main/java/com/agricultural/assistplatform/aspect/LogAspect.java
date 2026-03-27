package com.agricultural.assistplatform.aspect;

import com.agricultural.assistplatform.annotation.Log;
import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.SysOperationLog;
import com.agricultural.assistplatform.mapper.SysOperationLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private static final String ADMIN_CONTROLLER_PACKAGE = "com.agricultural.assistplatform.controller.admin";

    private final SysOperationLogMapper logMapper;

    @AfterReturning(
            pointcut = "execution(* com.agricultural.assistplatform.controller.admin..*(..)) || @annotation(com.agricultural.assistplatform.annotation.Log)",
            returning = "result"
    )
    public void saveLog(JoinPoint joinPoint, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Log logAnnotation = method.getAnnotation(Log.class);
            boolean adminMutation = isAdminMutation(joinPoint, method);
            if (logAnnotation == null && !adminMutation) {
                return;
            }
            if (result instanceof Result<?> r && r.getCode() != 200) {
                return;
            }

            LogMeta meta = resolveLogMeta(joinPoint, method, logAnnotation, result);
            if (!StringUtils.hasText(meta.businessType) || !StringUtils.hasText(meta.operationType)) {
                return;
            }

            SysOperationLog sysLog = new SysOperationLog();
            Long userId = LoginContext.getUserId();
            String loginType = LoginContext.getLoginType();
            String username = loginType != null ? loginType + "_" + userId : null;
            if (userId == null) {
                userId = 0L;
                username = "system";
            }

            sysLog.setOperatorId(userId);
            sysLog.setOperatorName(username);
            sysLog.setBusinessType(meta.businessType);
            sysLog.setOperationType(meta.operationType);
            sysLog.setBusinessId(meta.businessId);
            sysLog.setOperationContent(meta.content);
            sysLog.setIp(resolveClientIp());
            sysLog.setCreateTime(LocalDateTime.now());
            logMapper.insert(sysLog);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    private boolean isAdminMutation(JoinPoint joinPoint, Method method) {
        Class<?> targetClass = joinPoint.getTarget() != null ? joinPoint.getTarget().getClass() : method.getDeclaringClass();
        if (targetClass == null || targetClass.getName() == null || !targetClass.getName().startsWith(ADMIN_CONTROLLER_PACKAGE)) {
            return false;
        }
        return AnnotationUtils.findAnnotation(method, PostMapping.class) != null
                || AnnotationUtils.findAnnotation(method, PutMapping.class) != null
                || AnnotationUtils.findAnnotation(method, DeleteMapping.class) != null;
    }

    private LogMeta resolveLogMeta(JoinPoint joinPoint, Method method, Log logAnnotation, Object result) {
        LogMeta meta = new LogMeta();
        String fullPath = resolveRequestPath(joinPoint, method);
        String summary = resolveOperationSummary(method);
        meta.businessId = extractBusinessId(joinPoint, method, result);
        meta.businessType = resolveBusinessType(logAnnotation, fullPath, joinPoint, method);
        meta.operationType = resolveOperationType(logAnnotation, method, summary, fullPath);
        meta.content = resolveContent(logAnnotation, summary, meta.businessId, fullPath);
        return meta;
    }

    private String resolveBusinessType(Log logAnnotation, String fullPath, JoinPoint joinPoint, Method method) {
        if (logAnnotation != null && StringUtils.hasText(logAnnotation.businessType())) {
            return logAnnotation.businessType().trim();
        }
        String normalized = fullPath == null ? "" : fullPath.trim();
        if (normalized.startsWith("/admin/")) {
            normalized = normalized.substring("/admin/".length());
        } else if (normalized.startsWith("/admin")) {
            normalized = normalized.substring("/admin".length());
        }
        normalized = normalized.replaceAll("^/+", "");
        if (StringUtils.hasText(normalized)) {
            String[] segments = normalized.split("/");
            if (segments.length > 0 && StringUtils.hasText(segments[0])) {
                return segments[0].trim();
            }
        }
        String simpleName = joinPoint.getTarget() != null ? joinPoint.getTarget().getClass().getSimpleName() : method.getDeclaringClass().getSimpleName();
        return simpleName.replace("Admin", "").replace("Controller", "").toLowerCase();
    }

    private String resolveOperationType(Log logAnnotation, Method method, String summary, String fullPath) {
        if (logAnnotation != null && StringUtils.hasText(logAnnotation.operationType())) {
            return logAnnotation.operationType().trim();
        }
        String lowerPath = fullPath == null ? "" : fullPath.toLowerCase();
        String summaryText = summary == null ? "" : summary;
        if (AnnotationUtils.findAnnotation(method, DeleteMapping.class) != null || summaryText.contains("删除")) {
            return "删除";
        }
        if (summaryText.contains("审核") || lowerPath.contains("/audit")) {
            return "审核";
        }
        if (summaryText.contains("登录") || lowerPath.contains("/login")) {
            return "登录";
        }
        if (summaryText.contains("新增") || AnnotationUtils.findAnnotation(method, PostMapping.class) != null) {
            return "新增";
        }
        if (summaryText.contains("修改")
                || summaryText.contains("更新")
                || summaryText.contains("处理")
                || summaryText.contains("恢复")
                || summaryText.contains("打款")
                || summaryText.contains("发送")
                || AnnotationUtils.findAnnotation(method, PutMapping.class) != null) {
            return "修改";
        }
        return "操作";
    }

    private String resolveContent(Log logAnnotation, String summary, Long businessId, String fullPath) {
        String content = null;
        if (logAnnotation != null && StringUtils.hasText(logAnnotation.content())) {
            content = logAnnotation.content().trim();
            content = content.replace("{id}", String.valueOf(businessId == null ? 0L : businessId));
        }
        if (!StringUtils.hasText(content)) {
            content = StringUtils.hasText(summary) ? summary.trim() : "管理员执行操作";
            if (businessId != null && businessId > 0) {
                content = content + "，对象ID=" + businessId;
            } else if (StringUtils.hasText(fullPath)) {
                content = content + "，接口=" + fullPath;
            }
        }
        return content;
    }

    private String resolveRequestPath(JoinPoint joinPoint, Method method) {
        Class<?> targetClass = joinPoint.getTarget() != null ? joinPoint.getTarget().getClass() : method.getDeclaringClass();
        String classPath = extractMappingPath(AnnotationUtils.findAnnotation(targetClass, RequestMapping.class));
        String methodPath = extractMethodPath(method);
        if (!StringUtils.hasText(classPath)) {
            return methodPath;
        }
        if (!StringUtils.hasText(methodPath)) {
            return classPath;
        }
        return (classPath + "/" + methodPath).replaceAll("//+", "/");
    }

    private String extractMethodPath(Method method) {
        PostMapping postMapping = AnnotationUtils.findAnnotation(method, PostMapping.class);
        if (postMapping != null) return extractFirst(postMapping.value(), postMapping.path());
        PutMapping putMapping = AnnotationUtils.findAnnotation(method, PutMapping.class);
        if (putMapping != null) return extractFirst(putMapping.value(), putMapping.path());
        DeleteMapping deleteMapping = AnnotationUtils.findAnnotation(method, DeleteMapping.class);
        if (deleteMapping != null) return extractFirst(deleteMapping.value(), deleteMapping.path());
        RequestMapping requestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        return extractMappingPath(requestMapping);
    }

    private String extractMappingPath(RequestMapping requestMapping) {
        if (requestMapping == null) return "";
        return extractFirst(requestMapping.value(), requestMapping.path());
    }

    private String extractFirst(String[] values, String[] paths) {
        if (values != null && values.length > 0 && StringUtils.hasText(values[0])) {
            return values[0].trim();
        }
        if (paths != null && paths.length > 0 && StringUtils.hasText(paths[0])) {
            return paths[0].trim();
        }
        return "";
    }

    private String resolveOperationSummary(Method method) {
        Operation operation = AnnotationUtils.findAnnotation(method, Operation.class);
        return operation != null && StringUtils.hasText(operation.summary()) ? operation.summary().trim() : method.getName();
    }

    private Long extractBusinessId(JoinPoint joinPoint, Method method, Object result) {
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            Annotation[] annotations = parameters[i].getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == PathVariable.class) {
                    Long id = extractIdFromObject(args[i]);
                    if (id != null) return id;
                }
            }
        }
        for (Object arg : args) {
            Long id = extractIdFromObject(arg);
            if (id != null) return id;
        }
        return extractIdFromObject(result);
    }

    @SuppressWarnings("unchecked")
    private Long extractIdFromObject(Object source) {
        if (source == null) return null;
        if (source instanceof Number number) {
            return number.longValue();
        }
        if (source instanceof Result<?> result) {
            return extractIdFromObject(result.getData());
        }
        if (source instanceof Map<?, ?> map) {
            Object idValue = map.get("id");
            if (idValue == null) idValue = map.get("businessId");
            if (idValue == null) idValue = map.get("userId");
            if (idValue == null) idValue = map.get("merchantId");
            if (idValue instanceof Number number) return number.longValue();
            if (idValue instanceof String text && text.matches("\\d+")) return Long.parseLong(text);
            return null;
        }
        try {
            Method getId = source.getClass().getMethod("getId");
            Object value = getId.invoke(source);
            if (value instanceof Number number) {
                return number.longValue();
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private String resolveClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return null;
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static class LogMeta {
        private String businessType;
        private String operationType;
        private String content;
        private Long businessId;
    }
}
