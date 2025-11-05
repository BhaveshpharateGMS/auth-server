package com.gms_server.auth_app.controllers;

import com.gms_server.auth_app.services.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================
 * IDEMPOTENCY CONTROLLER - Industry Standard Pattern
 * ============================================
 *
 * Implements Stripe/AWS-style idempotency for API endpoints.
 *
 * FLOW:
 * 1. Client sends request with X-Idempotency-Key header
 * 2. /check endpoint validates and initiates idempotency
 * 3. Business logic processes (if first request)
 * 4. Response is cached automatically
 * 5. Duplicate requests return cached response
 *
 * RESPONSES:
 * - 200 OK: First request (allow) OR cached response (completed)
 * - 409 Conflict: Request still processing (duplicate blocked)
 * - 400 Bad Request: Missing/invalid idempotency key
 * - 500 Internal Error: Redis/system failure
 */
@RestController
@RequestMapping("/api/v1/idempotency")
public class IdempotencyController {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyController.class);

    private final IdempotencyService idempotencyService;

    public IdempotencyController(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
        logger.info("✓ IdempotencyController initialized - Endpoint: /check");
    }

    /**
     * ============================================
     * MAIN IDEMPOTENCY CHECK ENDPOINT
     * ============================================
     *
     * Called by Nginx auth_request or directly by clients.
     *
     * BEHAVIOR:
     * ✅ FIRST REQUEST:
     *    - Atomically stores "PROCESSING" in Redis
     *    - Returns 200 OK with {"status": "allowed"}
     *    - Backend proceeds with business logic
     *
     * ⚠️ DUPLICATE WHILE PROCESSING:
     *    - Finds "PROCESSING" in Redis
     *    - Returns 409 Conflict
     *    - Backend NEVER called
     *
     * ✅ DUPLICATE AFTER COMPLETION:
     *    - Finds cached response in Redis
     *    - Returns 200 OK with cached data
     *    - Backend NEVER called
     *
     * @param idempotencyKey Unique identifier from client (UUID recommended)
     * @return ResponseEntity with appropriate status and data
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> check(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        logger.info(">>> IDEMPOTENCY CHECK | Key: {}", idempotencyKey);

        Map<String, Object> response = new HashMap<>();

        // ============================================
        // STEP 1: VALIDATE IDEMPOTENCY KEY
        // ============================================
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            logger.warn("❌ VALIDATION FAILED | Missing idempotency key in X-Idempotency-Key header");
            response.put("error", "Idempotency key is required");
            response.put("message", "Please provide X-Idempotency-Key header with a unique UUID");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            // ============================================
            // STEP 2: CHECK FOR EXISTING CACHED RESPONSE
            // ============================================
            Map<String, Object> cached = idempotencyService.getCachedResponse(idempotencyKey);

            if (cached != null) {
                String status = (String) cached.get("status");

                // Case A: Request still processing
                if ("PROCESSING".equals(status)) {
                    logger.info("⏳ REQUEST BLOCKED | Status: PROCESSING | Key: {}", idempotencyKey);
                    logger.info("<<< Response: 409 CONFLICT");
                    logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                    response.put("status", "processing");
                    response.put("message", "Your request is currently being processed. Please wait.");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }

                // Case B: Request completed - return cached response
//                if ("COMPLETED".equals(status)) {
//                    Object cachedResponse = cached.get("response");
//                    logger.info("✅ CACHED RESPONSE RETURNED | Key: {}", idempotencyKey);
//                    logger.info("<<< Response: 200 OK (cached)");
//                    logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
//
//                    return ResponseEntity.ok(cachedResponse);
//                }

                if ("COMPLETED".equals(status)) {
                    Object cachedResponse = cached.get("response");
                    logger.info("✅ CACHED RESPONSE RETURNED | Key: {}", idempotencyKey);
                    logger.info("<<< Response: 200 OK (cached)");
                    logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                    // Safe cast with suppression (Redis returns LinkedHashMap which is a Map)
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = (Map<String, Object>) cachedResponse;
                    return ResponseEntity.ok(responseMap);
                }
            }

            // ============================================
            // STEP 3: INITIATE NEW REQUEST (ATOMIC)
            // ============================================
            Boolean initiated = idempotencyService.initiateIdempotencyKey(idempotencyKey);

            if (Boolean.TRUE.equals(initiated)) {
                // First request - ALLOW processing
                logger.info("✅ REQUEST ALLOWED | First request detected | Key: {}", idempotencyKey);
                logger.info("<<< Response: 200 OK (processing started)");
                logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                response.put("status", "allowed");
                response.put("message", "Request accepted for processing");
                return ResponseEntity.ok(response);
            } else {
                // Race condition: Key was created between check and initiate
                logger.warn("⚠️ RACE CONDITION DETECTED | Key created by concurrent request | Key: {}", idempotencyKey);
                logger.info("<<< Response: 409 CONFLICT");
                logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                response.put("status", "processing");
                response.put("message", "Your request is currently being processed. Please wait.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

        } catch (Exception e) {
            logger.error("❌ INTERNAL ERROR | Key: {} | Error: {}", idempotencyKey, e.getMessage(), e);
            logger.info("<<< Response: 500 INTERNAL SERVER ERROR");
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            response.put("error", "Internal server error");
            response.put("message", "An unexpected error occurred. Please try again later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * ============================================
     * STORE RESPONSE ENDPOINT
     * ============================================
     *
     * Called by backend after successful business logic completion.
     * Stores the final response, overwriting "PROCESSING" status.
     *
     * This should be called from your business logic controller AFTER
     * successful processing (e.g., order created, payment processed).
     *
     * @param idempotencyKey Unique identifier from client
     * @param responseBody The response data to cache
     * @return ResponseEntity confirming storage
     */
    @PostMapping("/response")
    public ResponseEntity<Map<String, String>> storeResponse(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestBody Object responseBody) {

        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        logger.info(">>> STORE RESPONSE | Key: {}", idempotencyKey);

        Map<String, String> response = new HashMap<>();

        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            logger.warn("❌ Missing idempotency key");
            response.put("error", "Idempotency key is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            idempotencyService.storeResponse(idempotencyKey, responseBody);

            logger.info("✅ Response stored successfully | Key: {}", idempotencyKey);
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            response.put("status", "success");
            response.put("message", "Response cached successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("❌ Error storing response | Key: {} | Error: {}", idempotencyKey, e.getMessage(), e);
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            response.put("error", "Failed to store response");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * ============================================
     * DELETE ENDPOINT (TESTING ONLY)
     * ============================================
     *
     * ⚠️ NOT RECOMMENDED FOR PRODUCTION
     *
     * Manually deletes an idempotency key.
     * Only use for testing/debugging.
     *
     * In production, keys auto-expire after TTL (15 minutes).
     *
     * @param idempotencyKey Unique identifier to delete
     * @return ResponseEntity confirming deletion
     */
    @DeleteMapping("/check")
    public ResponseEntity<Map<String, String>> delete(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey) {

        logger.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        logger.warn(">>> DELETE REQUEST (TESTING ONLY) | Key: {}", idempotencyKey);

        Map<String, String> response = new HashMap<>();

        try {
            idempotencyService.deleteIdempotencyKey(idempotencyKey);

            logger.warn("⚠️ Key deleted | Key: {}", idempotencyKey);
            logger.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            response.put("status", "deleted");
            response.put("message", "Idempotency key deleted (testing only)");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error deleting key | Key: {} | Error: {}", idempotencyKey, e.getMessage(), e);
            logger.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            response.put("error", "Delete failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * ============================================
     * HEALTH CHECK ENDPOINT
     * ============================================
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Idempotency Service");
        response.put("pattern", "Stripe/AWS-style");
        response.put("ttl", "15 minutes");
        return ResponseEntity.ok(response);
    }
}
