package com.gms_server.auth_app.services;

import com.gms_server.auth_app.configs.PersonaConfig;
import com.gms_server.auth_app.utils.PkceService;
import com.gms_server.auth_app.utils.RedisService;
import com.gms_server.auth_app.utils.ZitadelApiService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private final RedisService redisService;
    private final PersonaService personaService;
    private final ZitadelApiService zitadelApiService;

    // Cookie security configuration - set to false for localhost development
    @Value("${cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Strict}")
    private String cookieSameSite;

    // Session TTL - configurable via properties
    @Value("${session.ttl.days:7}")
    private int sessionTtlDays;

    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    public AuthenticationService(PersonaService personaService, RedisService redisService, ZitadelApiService zitadelApiService) {
        this.personaService = personaService;
        this.redisService = redisService;
        this.zitadelApiService = zitadelApiService;
    }

//    public ResponseEntity<Void> startAuthentication(String persona){
//        persona = persona.toLowerCase().trim();
//        PersonaConfig config = personaService.getPersonaConfig(persona);
//
//        final String codeVerifier = PkceService.generateCodeVerifier();
//        final String codeChallenge = PkceService.generateCodeChallenge(codeVerifier);
//        final String state = PkceService.generateState();
//        logger.info("codeVerify : {}",codeVerifier);
//        logger.info("codeChallenge : {}",codeChallenge);
//        logger.info("state : {}",state);
//
//        redisService.setValueWithExpiry(state, codeVerifier + ":" + persona, 10, java.util.concurrent.TimeUnit.MINUTES);
//        logger.info("save state {}", state);
//
//        logger.info("config {} ",config.toString());
//        String scope="openid profile email offline_access "+"urn:zitadel:iam:org:id:"+config.getOrganizationId();
//        URI authUri = UriComponentsBuilder.fromUriString(config.getIssuer() + "/oauth/v2/authorize")
//                .queryParam("client_id", config.getClientId())
//                .queryParam("response_type", "code")
//                .queryParam("scope", "openid profile email offline_access")
//                .queryParam("redirect_uri", config.getRedirectUri())
//                .queryParam("code_challenge", codeChallenge)
//                .queryParam("code_challenge_method", "S256")
//                .queryParam("state", state)
//                .build()
//                .toUri();
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setLocation(authUri);
//
//        return new ResponseEntity<>(headers, HttpStatus.FOUND);
//    }


    public ResponseEntity<Void> startAuthentication(String persona) {
        logger.info("🚀 [AUTH] Starting authentication flow for persona: {}", persona);
        
        persona = persona.toLowerCase().trim();
        PersonaConfig config = personaService.getPersonaConfig(persona);

        final String codeVerifier = PkceService.generateCodeVerifier();
        final String codeChallenge = PkceService.generateCodeChallenge(codeVerifier);
        final String state = PkceService.generateState();

        // SECURITY FIX: Don't log sensitive PKCE values in production
        if (logger.isDebugEnabled()) {
            logger.debug("🔐 [AUTH] Generated PKCE challenge (length: {}, method: S256)", codeChallenge.length());
            logger.debug("🎲 [AUTH] Generated state (length: {})", state.length());
        }

        redisService.setValueWithExpiry(state, codeVerifier + ":" + persona, 10, java.util.concurrent.TimeUnit.MINUTES);
        logger.info("💾 [AUTH] Stored authentication state in Redis (TTL: 10 minutes)");

        logger.debug("⚙️ [AUTH] Loaded configuration for persona: {}", persona);

        // Build the scope with organization ID
        String scope = "openid profile email offline_access " +
                "urn:zitadel:iam:org:id:" + config.getOrganizationId();

        logger.info("📋 [AUTH] Using scope with organization: {}", config.getOrganizationId());

        URI authUri = UriComponentsBuilder.fromUriString(config.getIssuer() + "/oauth/v2/authorize")
                .queryParam("client_id", config.getClientId())
                .queryParam("response_type", "code")
                .queryParam("scope", scope)
                .queryParam("redirect_uri", config.getRedirectUri())
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .queryParam("state", state)
                .build()
                .toUri();

        logger.info("🔗 [AUTH] Redirecting to authorization endpoint: {}", config.getIssuer());

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(authUri);

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }


    public void handleCallback(String code, String state, String error, HttpServletResponse response) {
        logger.info("🔄 [CALLBACK] Received OAuth callback");
        
        try {
            if (error != null) {
                logger.error("❌ [CALLBACK] OAuth error: {}", error);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "OAuth Error: " + error);
                return;
            }

            if (code == null || state == null) {
                logger.error("❌ [CALLBACK] Missing required parameters (code or state)");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing code or state");
                return;
            }

            logger.debug("🔍 [CALLBACK] Validating state parameter");
            String storedValue = redisService.getValue(state, String.class);

            if (storedValue == null) {
                logger.error("❌ [CALLBACK] Invalid or expired state parameter");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or expired state");
                return;
            }
            
            logger.info("✅ [CALLBACK] State validated successfully");
            redisService.delete(state);
            logger.debug("🗑️ [CALLBACK] Deleted used state from Redis");

            String[] parts = storedValue.split(":");
            if (parts.length != 2) {
                logger.error("❌ [CALLBACK] Invalid state format in Redis");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid state format");
                return;
            }

            String codeVerifier = parts[0];
            String persona = parts[1];
            
            // SECURITY FIX: Don't log sensitive code verifier
            logger.info("👤 [CALLBACK] Processing callback for persona: {}", persona);

            PersonaConfig config = personaService.getPersonaConfig(persona);
            logger.debug("⚙️ [CALLBACK] Loaded persona configuration");

            logger.info("🔄 [CALLBACK] Exchanging authorization code for tokens");
            @SuppressWarnings("unchecked")
            Map<String, Object> tokens = zitadelApiService.exchangeCodeForTokens(code, codeVerifier, config).block();
            if (tokens == null || !tokens.containsKey("access_token")) {
                logger.error("❌ [CALLBACK] Failed to exchange authorization code for tokens");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to get access token");
                return;
            }
            logger.info("✅ [CALLBACK] Successfully exchanged authorization code for tokens");
            String accessToken = (String) tokens.get("access_token");

            // Get userInfo using accessToken
            logger.info("👤 [CALLBACK] Retrieving user information");
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) zitadelApiService.getUserInfo(accessToken, config.getIssuer()).block();

            if (userInfo == null) {
                logger.error("❌ [CALLBACK] Failed to retrieve user information");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to get user info");
                return;
            }

            String userId = userInfo.get("sub").toString();
            logger.info("✅ [CALLBACK] User authenticated - ID: {}", userId);

            if (!personaService.hasPersonaRole(userInfo, config.getProjectId(), persona)) {
                logger.info("🔧 [CALLBACK] User missing {} role, assigning for user: {}", persona, userId);
                logger.debug("📋 [CALLBACK] Using project ID: {}", config.getProjectId());

                Boolean roleAssigned = zitadelApiService.assignRoleIfNeeded(userId, config.getIssuer(), config.getProjectId(), config.getManagementToken(), persona).block();
                logger.info("✅ [CALLBACK] Role assignment completed: {}", roleAssigned);

                // Generate new tokens using refreshToken
                String refreshToken = (String) tokens.get("refresh_token");
                logger.info("🔄 [CALLBACK] Refreshing tokens after role assignment");
                @SuppressWarnings("unchecked")
                Map<String, Object> newTokens = (Map<String, Object>) zitadelApiService.refreshTokens(refreshToken, config).block();
                if (newTokens != null) {
                    tokens = newTokens;
                    logger.info("✅ [CALLBACK] Tokens refreshed successfully after role assignment");
                } else {
                    logger.warn("⚠️ [CALLBACK] Token refresh returned null after role assignment");
                }
            } else {
                logger.info("✅ [CALLBACK] User already has {} role", persona);
            }

            // Create session and store tokens
            String sessionId = PkceService.generateSessionId();
            logger.info("🆔 [CALLBACK] Created session ID: {}", sessionId);
            String redisKey = "session:" + sessionId;
            
            int sessionTtlSeconds = sessionTtlDays * SECONDS_PER_DAY;
            redisService.setValueWithExpiry(redisKey, tokens, sessionTtlDays, TimeUnit.DAYS);
            logger.info("💾 [CALLBACK] Session stored in Redis (TTL: {} days)", sessionTtlDays);

            // Create SECURE cookie using ResponseCookie
            ResponseCookie cookie = ResponseCookie
                    .from(config.getSessionIdName(), sessionId)
                    .httpOnly(true)          // Prevents JavaScript access (XSS protection)
                    .secure(cookieSecure)    // HTTPS only (set false for localhost)
                    .path("/")               // Cookie available on all paths
                    .maxAge(sessionTtlSeconds)  // Matches Redis TTL
                    .sameSite(cookieSameSite)     // Prevents CSRF attacks (Strict/Lax/None)
                    .build();

            // Add secure cookie to response
            response.addHeader("Set-Cookie", cookie.toString());
            logger.info("🍪 [CALLBACK] Secure cookie created: name={}, maxAge={}s, secure={}, sameSite={}",
                    config.getSessionIdName(), sessionTtlSeconds, cookieSecure, cookieSameSite);

            String redirectUrl = personaService.getCallbackUrl(persona);
            logger.info("🔗 [CALLBACK] Redirecting to: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
            
            logger.info("✅ [CALLBACK] Authentication flow completed successfully for persona: {}", persona);
        } catch (Exception e) {
            logger.error("❌ [CALLBACK] Authentication failed: {}", e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication failed");
            } catch (Exception ex) {
                logger.error("❌ [CALLBACK] Failed to send error response", ex);
            }
        }
    }

    /**
     * Logout functionality - clears session from Redis and cookie
     */
    public java.util.Map<String, String> logout(String persona, jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) {
        logger.info("🚪 [LOGOUT] Starting logout for persona: {}", persona);
        java.util.Map<String, String> result = new java.util.HashMap<>();
        
        try {
            if (!personaService.isValidPersona(persona)) {
                logger.error("❌ [LOGOUT] Invalid persona: {}", persona);
                result.put("error", "Invalid persona");
                return result;
            }
            
            PersonaConfig config = personaService.getPersonaConfig(persona);
            String cookieName = config.getSessionIdName();
            String sessionId = null;
            
            // Get session ID from cookie
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if (cookieName.equals(cookie.getName())) {
                        sessionId = cookie.getValue();
                        logger.debug("🍪 [LOGOUT] Found session cookie: {}", cookieName);
                        break;
                    }
                }
            }
            
            // Delete session from Redis
            if (sessionId != null && !sessionId.trim().isEmpty()) {
                String redisKey = "session:" + sessionId;
                Boolean deleted = redisService.delete(redisKey);
                logger.info("💾 [LOGOUT] Session deleted from Redis: {}, success: {}", sessionId, deleted);
            } else {
                logger.warn("⚠️ [LOGOUT] No session ID found in cookies");
            }
            
            // Clear cookie
            ResponseCookie cookie = ResponseCookie
                    .from(cookieName, "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(0)  // Expire immediately
                    .sameSite(cookieSameSite)
                    .build();
            
            response.addHeader("Set-Cookie", cookie.toString());
            logger.info("🍪 [LOGOUT] Cookie cleared: {}", cookieName);
            
            logger.info("✅ [LOGOUT] Logout successful for persona: {}", persona);
            result.put("success", "true");
            result.put("message", "Logged out successfully");
            result.put("redirect_uri", config.getLogoutRedirectUri());
            
        } catch (Exception e) {
            logger.error("❌ [LOGOUT] Logout failed for persona: {}", persona, e);
            result.put("error", "Logout failed");
        }
        
        return result;
    }
}


