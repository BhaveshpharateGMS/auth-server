package com.gms_server.auth_app.configs;

import com.gms_server.auth_app.utils.RedisService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * ============================================
 * WEB SECURITY CONFIGURATION
 * ============================================
 * 
 * Provides:
 * 1. CORS configuration
 * 2. Security headers (XSS, CSP, etc.)
 * 3. Redis-based distributed rate limiting (works across ALL containers)
 */
@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Value("${cors.allowed.origins}")
    private String[] allowedOrigins;

    @Value("${cors.allowed.methods}")
    private String[] allowedMethods;

    @Value("${cors.allowed.headers}")
    private String[] allowedHeaders;

    @Value("${cors.allow.credentials}")
    private boolean allowCredentials;

    /**
     * Validate CORS Configuration on Startup
     * Ensures strict CORS control and logs allowed origins
     */
    @PostConstruct
    public void validateCorsConfiguration() {
        if (allowedOrigins == null || allowedOrigins.length == 0) {
            throw new IllegalStateException("🚨 [SECURITY] CORS allowed origins MUST be configured!");
        }
        
        // Log configured origins for visibility and security audit
        logger.warn("═══════════════════════════════════════════════════════════");
        logger.warn("🔒 [SECURITY] CORS is STRICTLY configured for these origins ONLY:");
        for (String origin : allowedOrigins) {
            logger.warn("   ✓ {}", origin);
        }
        logger.warn("🚫 [SECURITY] ALL other origins will be BLOCKED!");
        logger.warn("═══════════════════════════════════════════════════════════");
    }

    /**
     * CORS Configuration for Spring MVC
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        logger.info("⚙️ [CONFIG] Configuring CORS with origins: {}", Arrays.toString(allowedOrigins));
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods(allowedMethods)
                .allowedHeaders(allowedHeaders)
                .allowCredentials(allowCredentials)
                .maxAge(3600);
    }

    /**
     * CORS Configuration Source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList(allowedMethods));
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    /**
     * Security Headers Filter
     * Adds essential security headers to all responses
     */
    @Bean
    public SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    /**
     * DISTRIBUTED Rate Limiting Filter using Redis
     * ✅ Works across ALL containers/instances
     */
    @Bean
    public RedisRateLimitingFilter redisRateLimitingFilter(
            RedisService redisService,
            @Value("${rate.limit.enabled:true}") boolean enabled,
            @Value("${rate.limit.requests.per.minute:10}") int requestsPerMinute) {
        return new RedisRateLimitingFilter(redisService, enabled, requestsPerMinute);
    }

    /**
     * ============================================
     * SECURITY HEADERS FILTER
     * ============================================
     * 
     * Adds security headers to prevent:
     * - XSS attacks
     * - Clickjacking
     * - MIME sniffing
     * - Information disclosure
     */
    public static class SecurityHeadersFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                @NonNull HttpServletRequest request,
                @NonNull HttpServletResponse response,
                @NonNull FilterChain filterChain) throws ServletException, IOException {

            // Prevent clickjacking
            response.setHeader("X-Frame-Options", "DENY");

            // Prevent MIME sniffing
            response.setHeader("X-Content-Type-Options", "nosniff");

            // Enable XSS Protection
            response.setHeader("X-XSS-Protection", "1; mode=block");

            // Content Security Policy
            response.setHeader("Content-Security-Policy",
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self' data:; " +
                    "connect-src 'self'");

            // Referrer Policy
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

            // Permissions Policy
            response.setHeader("Permissions-Policy",
                    "geolocation=(), microphone=(), camera=()");

            // Strict Transport Security (only for HTTPS)
            if (request.isSecure()) {
                response.setHeader("Strict-Transport-Security",
                        "max-age=31536000; includeSubDomains");
            }

            filterChain.doFilter(request, response);
        }
    }

    /**
     * ============================================
     * REDIS-BASED RATE LIMITING FILTER
     * ============================================
     * 
     * ✅ MICROSERVICE-READY: Works across ALL containers
     * ✅ Uses Redis for shared counter (distributed state)
     * ✅ Atomic operations (no race conditions)
     * ✅ Automatic expiry (sliding window)
     * 
     * How it works in microservices:
     * 
     *   Container 1    Container 2    Container 3
     *        ↓              ↓              ↓
     *        └──────────────┼──────────────┘
     *                       ↓
     *                  Redis Server
     *              (Shared Rate Limit Counter)
     * 
     * All containers use the SAME Redis key for each client IP
     * So rate limiting is accurate across all instances!
     */
    public static class RedisRateLimitingFilter extends OncePerRequestFilter {

        private static final Logger logger = LoggerFactory.getLogger(RedisRateLimitingFilter.class);
        private static final String RATE_LIMIT_PREFIX = "ratelimit:";

        private final RedisService redisService;
        private final boolean enabled;
        private final int requestsPerMinute;

        public RedisRateLimitingFilter(RedisService redisService, boolean enabled, int requestsPerMinute) {
            this.redisService = redisService;
            this.enabled = enabled;
            this.requestsPerMinute = requestsPerMinute;
            logger.info("🛡️ [CONFIG] Redis-based Rate Limiting: {} (max {} req/min) - DISTRIBUTED ACROSS ALL CONTAINERS", 
                    enabled ? "ENABLED" : "DISABLED", requestsPerMinute);
        }

        @Override
        protected void doFilterInternal(
                @NonNull HttpServletRequest request,
                @NonNull HttpServletResponse response,
                @NonNull FilterChain filterChain) throws ServletException, IOException {

            if (!enabled) {
                filterChain.doFilter(request, response);
                return;
            }

            // Only rate limit authentication and verification endpoints
            String path = request.getRequestURI();
            if (!path.startsWith("/api/v1/auth/") && !path.startsWith("/api/v1/verify/")) {
                filterChain.doFilter(request, response);
                return;
            }

            String clientId = getClientIdentifier(request);
            String redisKey = RATE_LIMIT_PREFIX + clientId;

            try {
                // ═══════════════════════════════════════════════════════════════
                // ATOMIC INCREMENT: Thread-safe, distributed counter
                // ═══════════════════════════════════════════════════════════════
                // Uses Redis INCR command which is atomic - prevents race conditions
                // even with thousands of concurrent requests across multiple containers
                Long currentCount = redisService.increment(redisKey);
                
                if (currentCount == null) {
                    // This should never happen with INCR, but handle gracefully
                    logger.error("❌ [RATE-LIMIT] Failed to increment counter for client: {}", clientId);
                    filterChain.doFilter(request, response);
                    return;
                }

                // Set expiry on first request (TTL = 1 minute)
                if (currentCount == 1) {
                    redisService.expire(redisKey, 1, TimeUnit.MINUTES);
                    logger.debug("🆕 [RATE-LIMIT] Initialized counter for client: {} (1/{})", clientId, requestsPerMinute);
                }

                // Check if rate limit exceeded
                if (currentCount > requestsPerMinute) {
                    // Get TTL for Retry-After header
                    Long ttl = redisService.getTTL(redisKey);
                    int retryAfter = (ttl != null && ttl > 0) ? ttl.intValue() : 60;
                    
                    logger.warn("🚫 [RATE-LIMIT] EXCEEDED for client: {} ({}/{}) on path: {} | Retry in {}s", 
                            clientId, currentCount, requestsPerMinute, path, retryAfter);
                    
                    // Return 429 Too Many Requests
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
                    response.setHeader("X-RateLimit-Remaining", "0");
                    response.setHeader("X-RateLimit-Reset", String.valueOf(retryAfter));
                    response.setHeader("Retry-After", String.valueOf(retryAfter));
                    
                    response.getWriter().write(String.format(
                            "{\"error\": \"Rate limit exceeded\", " +
                            "\"message\": \"Too many requests. Please try again in %d seconds.\", " +
                            "\"limit\": %d, " +
                            "\"current\": %d, " +
                            "\"retry_after_seconds\": %d}",
                            retryAfter, requestsPerMinute, currentCount, retryAfter
                    ));
                    return;
                }

                // Request allowed - add informative headers
                int remaining = Math.max(0, requestsPerMinute - currentCount.intValue());
                Long ttl = redisService.getTTL(redisKey);
                
                logger.debug("✅ [RATE-LIMIT] Allowed for client: {} ({}/{}) | Remaining: {} | Reset in: {}s", 
                        clientId, currentCount, requestsPerMinute, remaining, ttl);
                
                response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
                response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
                response.setHeader("X-RateLimit-Reset", String.valueOf(ttl != null ? ttl : 60));
                
                filterChain.doFilter(request, response);

            } catch (Exception e) {
                logger.error("❌ [RATE-LIMIT] Error for client: {}, allowing request (fail-open)", clientId, e);
                // On error, allow the request (fail-open approach for availability)
                filterChain.doFilter(request, response);
            }
        }

        /**
         * Extract client identifier (IP address) from request
         * Handles proxy headers (X-Forwarded-For, X-Real-IP)
         * 
         * IMPORTANT for load balancers:
         * - X-Forwarded-For: Contains original client IP
         * - X-Real-IP: Alternative header for original IP
         * 
         * Sanitizes IP to create flat Redis keys (no nested folders)
         * Example: 0:0:0:0:0:0:0:1 → 0_0_0_0_0_0_0_1
         */
        private String getClientIdentifier(HttpServletRequest request) {
            // Priority: X-Forwarded-For > X-Real-IP > Remote Address
            String ip = request.getHeader("X-Forwarded-For");
            
            if (ip != null && !ip.isEmpty()) {
                // X-Forwarded-For can contain multiple IPs: "client, proxy1, proxy2"
                // Take the FIRST one (original client IP)
                ip = ip.split(",")[0].trim();
            } else {
                ip = request.getHeader("X-Real-IP");
            }
            
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            
            // Sanitize IP address
            if (ip == null || ip.isEmpty()) {
                ip = "unknown";
            }
            
            // Replace colons and dots with underscores to prevent Redis folder nesting
            // Before: ratelimit:0:0:0:0:0:0:0:1 (nested folders in Redis GUI)
            // After:  ratelimit:0_0_0_0_0_0_0_1 (single flat key)
            ip = ip.replace(":", "_").replace(".", "_");
            
            return ip;
        }
    }
}

