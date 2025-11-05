package com.gms_server.auth_app.services;

import com.gms_server.auth_app.utils.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ============================================
 * IDEMPOTENCY SERVICE - Industry Standard Pattern
 * ============================================
 *
 * Implements Stripe/AWS-style idempotency:
 * - First request: Store "PROCESSING" atomically
 * - During processing: Return 409 Conflict for duplicates
 * - After completion: Return cached response (200 OK)
 * - Auto-expiry: 15 minutes TTL (no manual delete needed)
 *
 * Benefits:
 * - Prevents duplicate processing
 * - Safe for client retries
 * - Consistent responses
 * - Race condition free (atomic operations)
 */
@Service
public class IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);

    private final RedisService redisService;

    // Configuration
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final String PROCESSING_STATUS = "PROCESSING";
    private static final long TTL_MINUTES = 15;

    public IdempotencyService(RedisService redisService) {
        this.redisService = redisService;
        logger.info("✓ IdempotencyService initialized - TTL: {} minutes, Pattern: Stripe/AWS-style", TTL_MINUTES);
    }

    /**
     * ============================================
     * STEP 1: INITIATE IDEMPOTENCY (ATOMIC)
     * ============================================
     *
     * Atomically stores "PROCESSING" status for new requests.
     * This is the FIRST step - called before any business logic.
     *
     * @param idempotencyKey Unique key from client (UUID recommended)
     * @return true if stored (first request - ALLOW), false if exists (duplicate - BLOCK)
     * @throws IllegalArgumentException if key is null/empty
     */
    public Boolean initiateIdempotencyKey(String idempotencyKey) {
        // Validation
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            logger.error("❌ Cannot initiate null or empty idempotency key");
            throw new IllegalArgumentException("Idempotency key cannot be null or empty");
        }

        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;

        // ATOMIC: Set only if key doesn't exist (prevents race conditions)
        Boolean stored = redisService.setIfAbsent(redisKey, PROCESSING_STATUS, TTL_MINUTES, TimeUnit.MINUTES);

        if (Boolean.TRUE.equals(stored)) {
            logger.info("✅ FIRST REQUEST - Idempotency key initiated | Key: {} | Status: PROCESSING | TTL: {}min",
                    idempotencyKey, TTL_MINUTES);
        } else {
            logger.warn("⚠️ DUPLICATE REQUEST - Idempotency key already exists | Key: {}", idempotencyKey);
        }

        return stored;
    }

    /**
     * ============================================
     * STEP 2: GET CACHED RESPONSE
     * ============================================
     *
     * Retrieves the current state of an idempotency key.
     * Used to check if request is still processing or completed.
     *
     * @param idempotencyKey Unique key from client
     * @return Map with "status" and optional "response" data, or null if not found
     */
    public Map<String, Object> getCachedResponse(String idempotencyKey) {
        // Validation
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            logger.warn("⚠️ Cannot get cached response for null or empty key");
            return null;
        }

        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;

        // Get value from Redis (can be "PROCESSING" string or response object)
        Object cachedData = redisService.getValue(redisKey, Object.class);

        if (cachedData == null) {
            logger.debug("🔍 No cached data found | Key: {}", idempotencyKey);
            return null;
        }

        // Check if still processing
        if (PROCESSING_STATUS.equals(cachedData)) {
            logger.info("⏳ Request PROCESSING | Key: {}", idempotencyKey);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "PROCESSING");
            return result;
        }

        // Response completed - return cached data
        logger.info("✅ CACHED RESPONSE found | Key: {}", idempotencyKey);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "COMPLETED");
        result.put("response", cachedData);
        return result;
    }

    /**
     * ============================================
     * STEP 3: STORE FINAL RESPONSE
     * ============================================
     *
     * Stores the successful business logic response.
     * This OVERWRITES the "PROCESSING" status with actual response data.
     * Called AFTER business logic completes successfully.
     *
     * Redis JSON serialization handles object → JSON automatically.
     *
     * @param idempotencyKey Unique key from client
     * @param responseData The response object to cache (Map, POJO, etc.)
     */
    public void storeResponse(String idempotencyKey, Object responseData) {
        // Validation
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            logger.warn("⚠️ Cannot store response for null or empty key");
            return;
        }

        if (responseData == null) {
            logger.warn("⚠️ Cannot store null response data | Key: {}", idempotencyKey);
            return;
        }

        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;

        try {
            // Store response with TTL (overwrites "PROCESSING")
            // Redis GenericJackson2JsonRedisSerializer handles serialization automatically
            redisService.setValueWithExpiry(redisKey, responseData, TTL_MINUTES, TimeUnit.MINUTES);

            logger.info("✅ Response STORED successfully | Key: {} | TTL: {}min", idempotencyKey, TTL_MINUTES);

        } catch (Exception e) {
            logger.error("❌ Error storing response | Key: {} | Error: {}", idempotencyKey, e.getMessage(), e);
        }
    }

    /**
     * ============================================
     * UTILITY: CHECK KEY EXISTS
     * ============================================
     *
     * Simple check if idempotency key exists in Redis.
     *
     * @param idempotencyKey Unique key from client
     * @return true if exists, false otherwise
     */
    public Boolean keyExists(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return false;
        }

        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        Boolean exists = redisService.hasKey(redisKey);

        logger.debug("🔍 Key existence check | Key: {} | Exists: {}", idempotencyKey, exists);
        return exists;
    }

    /**
     * ============================================
     * OPTIONAL: DELETE KEY (FOR TESTING ONLY)
     * ============================================
     *
     * ⚠️ NOT RECOMMENDED FOR PRODUCTION
     * Keys auto-expire after TTL. Manual deletion breaks idempotency guarantees.
     * Only use for testing/debugging.
     *
     * @param idempotencyKey Unique key from client
     */
    public void deleteIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            logger.warn("⚠️ Cannot delete null or empty idempotency key");
            return;
        }

        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        Boolean deleted = redisService.delete(redisKey);

        logger.warn("⚠️ Idempotency key DELETED (testing only) | Key: {} | Success: {}", idempotencyKey, deleted);
    }
}
