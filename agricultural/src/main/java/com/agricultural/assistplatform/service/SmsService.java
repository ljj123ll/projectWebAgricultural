package com.agricultural.assistplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 短信验证码（模拟：实际对接短信网关）
 * Redis key: sms:code:{phone}, 5分钟过期
 */
@Service
@RequiredArgsConstructor
public class SmsService {

    private static final String KEY_PREFIX = "sms:code:";
    private static final int EXPIRE_MINUTES = 5;
    /** 开发环境固定验证码，便于测试 */
    private static final String DEV_CODE = "123456";

    private final StringRedisTemplate redisTemplate;

    public void sendCode(String phone) {
        String code = DEV_CODE;
        redisTemplate.opsForValue().set(KEY_PREFIX + phone, code, EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    public boolean verifyCode(String phone, String code) {
        String key = KEY_PREFIX + phone;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) return false;
        boolean ok = cached.equals(code);
        if (ok) redisTemplate.delete(key);
        return ok;
    }
}
