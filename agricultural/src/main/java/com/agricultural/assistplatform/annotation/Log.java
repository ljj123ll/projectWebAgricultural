package com.agricultural.assistplatform.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 系统操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    /** 业务类型 */
    String businessType();
    
    /** 操作类型 */
    String operationType() default "UPDATE";
    
    /** 操作内容模板，如 "审核通过商品：{id}" */
    String content() default "";
}
