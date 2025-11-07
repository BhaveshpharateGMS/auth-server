package com.gms_server.auth_app.services;

import com.gms_server.auth_app.configs.PersonaConfig;
import com.gms_server.auth_app.utils.RedisService;
import com.gms_server.auth_app.utils.ZitadelApiService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);

    private final RedisService redisService;
    private final ZitadelApiService zitadelApiService;
    private final PersonaService personaService;
    private final CachingService cachingService;

    @Value("${session.ttl.days:7}")
    private int sessionTtlDays;

    public AuthorizationService(RedisService redisService,
                                ZitadelApiService zitadelApiService,
                                PersonaService personaService,
                                CachingService cachingService) {
        this.redisService = redisService;
        this.zitadelApiService = zitadelApiService;
        this.personaService = personaService;
        this.cachingService = cachingService;
    }

    /**
     * Verifies the persona authorization for the given request.
     * Returns user info if authorized, throws exception otherwise.
     */
    public Map<String, Object> verifyPersonaAuthorization(String persona, HttpServletRequest request) {
        logger.info("🔐 [VERIFY] Starting authorization for persona: {}", persona);

        // Validate persona
        if (!personaService.isValidPersona(persona)) {
            logger.error("❌ [VERIFY] Invalid persona: {}", persona);
            throw new AuthorizationException("Invalid persona", 400);
        }

        PersonaConfig config = personaService.getPersonaConfig(persona);
        
        // FIXED: Extract session ID from cookie (no hardcoded fallback!)
        //String sessionId = getSessionIdFromCookie(request, config);
        String sessionId = getSessionIdFromCookie(request, config);

        if (sessionId == null || sessionId.trim().isEmpty()) {
            logger.error("❌ [VERIFY] Session cookie not found for persona: {}", persona);
            throw new AuthorizationException("Session not found", 401);
        }

        logger.info("🔍 [VERIFY] Checking session for persona: {}, sessionId: {}", persona, sessionId);

        try {
            String redisKey = "session:" + sessionId;
            Map<String, Object> session = redisService.getValue(redisKey, Map.class);

            if (session == null) {
                logger.error("❌ [VERIFY] Session not found in Redis: {}", sessionId);
                throw new AuthorizationException("Session expired or invalid", 401);
            }

            String accessToken = (String) session.get("access_token");
            String refreshToken = (String) session.get("refresh_token");

            if (accessToken == null || refreshToken == null) {
                logger.error("❌ [VERIFY] Invalid session tokens for sessionId: {}", sessionId);
                throw new AuthorizationException("Invalid session tokens", 401);
            }

            logger.debug("🔑 [VERIFY] Retrieved tokens from Redis session");

            // Validate access token and get user info
            Map<String, Object> userInfo = cachingService.getUserInfoByToken(accessToken, config.getIssuer());

            if (userInfo == null) {
                logger.info("⏰ [VERIFY] Access token expired, refreshing for sessionId: {}", sessionId);
                Map<String, Object> newTokens = zitadelApiService.refreshTokens(refreshToken, config).block();

                if (newTokens == null) {
                    logger.error("❌ [VERIFY] Token refresh failed");
                    throw new AuthorizationException("Token refresh failed", 401);
                }

                session = newTokens;

                accessToken = (String) session.get("access_token");
                refreshToken = (String) session.get("refresh_token");

                if (accessToken == null || refreshToken == null) {
                    logger.error("❌ [VERIFY] Invalid refreshed tokens for sessionId: {}", sessionId);
                    throw new AuthorizationException("Invalid session tokens", 401);
                }

                // Update session in Redis with new tokens
                redisService.setValueWithExpiry(redisKey, session, sessionTtlDays, TimeUnit.DAYS);

                // Invalidate old token cache
                cachingService.invalidateToken(accessToken);

                logger.info("✅ [VERIFY] Tokens refreshed and saved to Redis");

                userInfo = cachingService.getUserInfoByToken(accessToken, config.getIssuer());
                if (userInfo == null) {
                    logger.error("❌ [VERIFY] Failed to get user info after token refresh");
                    throw new AuthorizationException("Failed to get user info after refresh", 401);
                }
            }

            // Verify persona role
            if (!personaService.hasPersonaRole(userInfo, config.getProjectId(), persona)) {
                logger.warn("⚠️ [VERIFY] Persona role '{}' missing, attempting token refresh", persona);

                Map<String, Object> newTokens = zitadelApiService.refreshTokens(refreshToken, config).block();
                if (newTokens == null) {
                    logger.error("❌ [VERIFY] Token refresh failed for role verification");
                    throw new AuthorizationException("Token refresh failed", 401);
                }

                session = newTokens;

                accessToken = (String) session.get("access_token");
                refreshToken = (String) session.get("refresh_token");

                if (accessToken == null || refreshToken == null) {
                    logger.error("❌ [VERIFY] Invalid tokens after role refresh");
                    throw new AuthorizationException("Invalid session tokens", 401);
                }

                // Update session with new tokens
                redisService.setValueWithExpiry(redisKey, session, sessionTtlDays, TimeUnit.DAYS);

                // Invalidate old token cache
                cachingService.invalidateToken(accessToken);

                userInfo = cachingService.getUserInfoByToken(accessToken, config.getIssuer());
                if (userInfo == null || !personaService.hasPersonaRole(userInfo, config.getProjectId(), persona)) {
                    logger.error("❌ [VERIFY] Persona role '{}' still missing after refresh", persona);
                    throw new AuthorizationException("Insufficient permissions", 403);
                }
            }

            logger.info("✅ [VERIFY] Authorization successful for persona: {}", persona);
            return userInfo;

        } catch (AuthorizationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("❌ [VERIFY] Unexpected authorization error for persona: {}", persona, e);
            throw new AuthorizationException("Authorization failed", 500);
        }
    }

    /**
     * FIXED: Properly extract session ID from cookie without hardcoded fallback
     */
    private String getSessionIdFromCookie(HttpServletRequest request, PersonaConfig config) {
        String cookieName = config.getSessionIdName();
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    String value = cookie.getValue();
                    if (value != null && !value.trim().isEmpty()) {
                        logger.debug("🍪 [VERIFY] Found session cookie: {}", cookieName);
                        return value;
                    }
                }
            }
        }
        
        // SECURITY FIX: No hardcoded fallback - return null if cookie not found
        logger.warn("⚠️ [VERIFY] Session cookie '{}' not found in request", cookieName);
        return null;
    }

    /**
     * Custom exception for authorization errors with HTTP status code.
     */
    public static class AuthorizationException extends RuntimeException {
        private final int statusCode;

        public AuthorizationException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}