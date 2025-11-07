package com.gms_server.auth_app.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, Object> hashOperations;

    public RedisService(RedisTemplate<String, Object> redisTemplate,
                        HashOperations<String, String, Object> hashOperations) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = hashOperations;
    }

    // -------- SET / GET with expiry --------
    public void setValue(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            logger.error("Error setting Redis key: {}", key, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    public void setValueWithExpiry(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofMillis(unit.toMillis(timeout)));
        } catch (Exception e) {
            logger.error("Error setting Redis key with expiry: {}", key, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    public <T> T getValue(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) return null;
            return clazz.cast(value);
        } catch (Exception e) {
            logger.error("Error getting Redis key: {}", key, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    // -------- HSET / HGET --------
    public void hset(String key, String field, Object value) {
        try {
            hashOperations.put(key, field, value);
        } catch (Exception e) {
            logger.error("Error setting Redis hash key: {}, field: {}", key, field, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    public <T> T hget(String key, String field, Class<T> clazz) {
        try {
            Object value = hashOperations.get(key, field);
            if (value == null) return null;
            return clazz.cast(value);
        } catch (Exception e) {
            logger.error("Error getting Redis hash key: {}, field: {}", key, field, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    public Map<String, Object> hgetAll(String key) {
        try {
            return hashOperations.entries(key);
        } catch (Exception e) {
            logger.error("Error getting all Redis hash fields for key: {}", key, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    // -------- DELETE --------
    public Boolean delete(String key) {
        try {
            logger.info("start delete state: {}",key);
            return redisTemplate.delete(key);
        } catch (Exception e) {
            logger.error("Error deleting Redis key: {}", key, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    public Long hdelete(String key, String... fields) {
        try {
            return hashOperations.delete(key, (Object[]) fields);
        } catch (Exception e) {
            logger.error("Error deleting Redis hash fields for key: {}", key, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    // -------- EXPIRE --------
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return redisTemplate.expire(key, Duration.ofMillis(unit.toMillis(timeout)));
        } catch (Exception e) {
            logger.error("Error setting expiry for Redis key: {}", key, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(
                key, 
                value, 
                Duration.ofMillis(unit.toMillis(timeout))
            );
            logger.debug("SET NX operation | Key: {} | Success: {}", key, result);
            return result;
        } catch (Exception e) {
            logger.error("Error setting Redis key with setIfAbsent: {}", key, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    public Boolean hasKey(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            logger.error("Error checking if Redis key exists: {}", key, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    // -------- ATOMIC INCREMENT --------
    /**
     * Atomically increment a key's value by 1
     * If key doesn't exist, it will be created with value 1
     * This operation is atomic and thread-safe
     * 
     * @param key Redis key
     * @return The new value after increment
     */
    public Long increment(String key) {
        try {
            Long newValue = redisTemplate.opsForValue().increment(key);
            logger.debug("🔢 [REDIS-INCR] Key: {} | New Value: {}", key, newValue);
            return newValue;
        } catch (Exception e) {
            logger.error("Error incrementing Redis key: {}", key, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    /**
     * Atomically increment a key's value by delta
     * If key doesn't exist, it will be created with value = delta
     * This operation is atomic and thread-safe
     * 
     * @param key Redis key
     * @param delta Amount to increment by
     * @return The new value after increment
     */
    public Long incrementBy(String key, long delta) {
        try {
            Long newValue = redisTemplate.opsForValue().increment(key, delta);
            logger.debug("🔢 [REDIS-INCR] Key: {} | Delta: {} | New Value: {}", key, delta, newValue);
            return newValue;
        } catch (Exception e) {
            logger.error("Error incrementing Redis key: {}", key, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    /**
     * Get time-to-live (TTL) for a key in seconds
     * 
     * @param key Redis key
     * @return TTL in seconds, -1 if key exists but has no expiry, -2 if key doesn't exist
     */
    public Long getTTL(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Error getting TTL for Redis key: {}", key, e);
            return -2L;
        }
    }
}