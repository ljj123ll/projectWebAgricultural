package com.agricultural.assistplatform.aspect;

import com.agricultural.assistplatform.annotation.Log;
import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.entity.SysOperationLog;
import com.agricultural.assistplatform.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final SysOperationLogMapper logMapper;

    @AfterReturning(pointcut = "@annotation(com.agricultural.assistplatform.annotation.Log)", returning = "result")
    public void saveLog(JoinPoint joinPoint, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Log logAnnotation = method.getAnnotation(Log.class);

            if (logAnnotation != null) {
                SysOperationLog sysLog = new SysOperationLog();
                
                // 获取当前登录用户
                Long userId = LoginContext.getUserId();
                String loginType = LoginContext.getLoginType();
                String username = loginType != null ? loginType + "_" + userId : null;
                if (userId == null) {
                    userId = 0L;
                    username = "system";
                }
                sysLog.setOperatorId(userId);
                sysLog.setOperatorName(username);
                
                sysLog.setBusinessType(logAnnotation.businessType());
                sysLog.setOperationType(logAnnotation.operationType());
                
                // 获取参数中的 ID (假设第一个参数是 ID，或者从对象中获取)
                Object[] args = joinPoint.getArgs();
                Long businessId = 0L;
                if (args.length > 0) {
                    if (args[0] instanceof Long) {
                        businessId = (Long) args[0];
                    }
                    // 这里可以根据实际情况解析更多类型的 ID
                }
                sysLog.setBusinessId(businessId);
                
                // 内容模板解析 (简单实现)
                String content = logAnnotation.content();
                if (content.contains("{id}")) {
                    content = content.replace("{id}", String.valueOf(businessId));
                }
                sysLog.setOperationContent(content);
                
                // 获取 IP
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    sysLog.setIp(attributes.getRequest().getRemoteAddr());
                }
                
                sysLog.setCreateTime(LocalDateTime.now());
                
                logMapper.insert(sysLog);
            }
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }
}
