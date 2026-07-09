package com.zjj.chatsystem.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 操作封装服务
 */
@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ========== String 操作 ==========

    public void set(String key, Object value, long ttl, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, ttl, unit);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    public boolean expire(String key, long ttl, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, ttl, unit));
    }

    // ========== Hash 操作 ==========

    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String field) {
        return (T) redisTemplate.opsForHash().get(key, field);
    }

    // ========== List 操作 ==========

    public <T> void lPush(String key, T value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> lRange(String key, long start, long end) {
        return (List<T>) (List<?>) redisTemplate.opsForList().range(key, start, end);
    }

    // ========== Set 操作 ==========

    @SafeVarargs
    public final <T> void sAdd(String key, T... values) {
        redisTemplate.opsForSet().add(key, values);
    }

    public Boolean sIsMember(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }
}
