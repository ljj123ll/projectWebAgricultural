package com.agricultural.assistplatform.service.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> T getOrLoad(String key, Duration ttl, Class<T> clazz, Supplier<T> loader) {
        return getOrLoadInternal(key, ttl, loader, () -> readValue(key, clazz));
    }

    public <T> T getOrLoad(String key, Duration ttl, TypeReference<T> typeReference, Supplier<T> loader) {
        return getOrLoadInternal(key, ttl, loader, () -> readValue(key, typeReference));
    }

    public void delete(String... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        for (String key : keys) {
            if (StringUtils.hasText(key)) {
                redisTemplate.delete(key);
            }
        }
    }

    public void deleteByPrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return;
        }
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void put(String key, Object value, Duration ttl) {
        if (!StringUtils.hasText(key) || value == null || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException e) {
            log.warn("写入 Redis 缓存失败, key={}", key, e);
        }
    }

    private <T> T getOrLoadInternal(String key, Duration ttl, Supplier<T> loader, Supplier<T> reader) {
        if (!StringUtils.hasText(key)) {
            return loader.get();
        }
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (StringUtils.hasText(cached)) {
                T value = reader.get();
                if (value != null) {
                    return value;
                }
            }
        } catch (Exception e) {
            log.warn("读取 Redis 缓存失败, key={}", key, e);
        }

        T data = loader.get();
        if (data != null) {
            put(key, data, ttl);
        }
        return data;
    }

    private <T> T readValue(String key, Class<T> clazz) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            return StringUtils.hasText(cached) ? objectMapper.readValue(cached, clazz) : null;
        } catch (Exception e) {
            log.warn("反序列化 Redis 缓存失败, key={}", key, e);
            redisTemplate.delete(key);
            return null;
        }
    }

    private <T> T readValue(String key, TypeReference<T> typeReference) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            return StringUtils.hasText(cached) ? objectMapper.readValue(cached, typeReference) : null;
        } catch (Exception e) {
            log.warn("反序列化 Redis 缓存失败, key={}", key, e);
            redisTemplate.delete(key);
            return null;
        }
    }
}
