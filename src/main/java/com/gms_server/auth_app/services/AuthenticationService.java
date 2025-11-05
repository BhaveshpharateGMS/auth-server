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

    // Session TTL in days - must match Redis TTL
    private static final int SESSION_TTL_DAYS = 7;
    private static final int SESSION_TTL_SECONDS = SESSION_TTL_DAYS * 24 * 60 * 60; // 604800 seconds

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
        persona = persona.toLowerCase().trim();
        PersonaConfig config = personaService.getPersonaConfig(persona);

        final String codeVerifier = PkceService.generateCodeVerifier();
        final String codeChallenge = PkceService.generateCodeChallenge(codeVerifier);
        final String state = PkceService.generateState();

        logger.info("codeVerify : {}", codeVerifier);
        logger.info("codeChallenge : {}", codeChallenge);
        logger.info("state : {}", state);

        redisService.setValueWithExpiry(state, codeVerifier + ":" + persona, 10, java.util.concurrent.TimeUnit.MINUTES);
        logger.info("save state {}", state);

        logger.info("config {} ", config.toString());

        // Build the scope with organization ID
        String scope = "openid profile email offline_access " +
                "urn:zitadel:iam:org:id:" + config.getOrganizationId();

        logger.info("Using scope: {}", scope); // Add this log to verify!

        URI authUri = UriComponentsBuilder.fromUriString(config.getIssuer() + "/oauth/v2/authorize")
                .queryParam("client_id", config.getClientId())
                .queryParam("response_type", "code")
                .queryParam("scope", scope)  // 🔥 USE THE VARIABLE HERE!
                .queryParam("redirect_uri", config.getRedirectUri())
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .queryParam("state", state)
                .build()
                .toUri();

        logger.info("Authorization URI: {}", authUri); // Log the full URI to verify

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(authUri);

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }


    public void handleCallback(String code, String state, String error, HttpServletResponse response) {
        try {
            if (error != null) {
                logger.error("OAuth error: {}", error);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "OAuth Error: " + error);
                return;
            }

            if (code == null || state == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing code or state");
                return;
            }

            String storedValue = redisService.getValue(state, String.class);

            if (storedValue == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid state");
                return;
            }
            logger.info("get redis storeValue: {}", storedValue);
            redisService.delete(state);

            String[] parts = storedValue.split(":");
            if (parts.length != 2) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid state format");
                return;
            }

            String codeVerifier = parts[0];
            String persona = parts[1];
            logger.info("codeVerifier: {}", codeVerifier);
            logger.info("persona: {}", persona);

            PersonaConfig config = personaService.getPersonaConfig(persona);
            logger.info("config: {}", config.toString());

            @SuppressWarnings("unchecked")
            Map<String, Object> tokens = zitadelApiService.exchangeCodeForTokens(code, codeVerifier, config).block();
            if (tokens == null || !tokens.containsKey("access_token")) {
                logger.error("Failed to exchange code for tokens");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to get access token");
                return;
            }
            logger.info("Successfully exchanged authorization code for tokens");
            String accessToken = (String) tokens.get("access_token");

            // Get userInfo using accessToken
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) zitadelApiService.getUserInfo(accessToken, config.getIssuer()).block();

            logger.info("Successfully retrieved user info from access token");

            if (userInfo == null) {
                logger.error("Failed to get user info");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to get user info");
                return;
            }

            String userId = userInfo.get("sub").toString();
            logger.info("User ID: {}", userId);

            if (!personaService.hasPersonaRole(userInfo, config.getProjectId(), persona)) {
                logger.info("User does not have {} role, assigning role for user: {}", persona, userId);
                logger.debug("Using project ID: {}", config.getProjectId());

                Boolean roleAssigned = zitadelApiService.assignRoleIfNeeded(userId, config.getIssuer(), config.getProjectId(), config.getManagementToken(), persona).block();
                logger.info("Role assignment result: {}", roleAssigned );

                // Generate new tokens using refreshToken
                String refreshToken = (String) tokens.get("refresh_token");
                @SuppressWarnings("unchecked")
                Map<String, Object> newTokens = (Map<String, Object>) zitadelApiService.refreshTokens(refreshToken, config).block();
                if (newTokens != null) {
                    tokens = newTokens;
                    logger.info("Tokens refreshed successfully after role assignment");
                }
            }

            // Create session and store tokens
            String sessionId = PkceService.generateSessionId();
            logger.info("Session created successfully with ID: {}", sessionId);
            String redisKey = "session:" + sessionId;
            redisService.setValueWithExpiry(redisKey, tokens, SESSION_TTL_DAYS, TimeUnit.DAYS);

            // Create SECURE cookie using ResponseCookie
            ResponseCookie cookie = ResponseCookie
                    .from(config.getSessionIdName(), sessionId)
                    .httpOnly(true)          // Prevents JavaScript access (XSS protection)
                    .secure(cookieSecure)    // HTTPS only (set false for localhost)
                    .path("/")               // Cookie available on all paths
                    .maxAge(SESSION_TTL_SECONDS)  // 7 days - matches Redis TTL
                    .sameSite(cookieSameSite)     // Prevents CSRF attacks (Strict/Lax/None)
                    .build();

            // Add secure cookie to response
            response.addHeader("Set-Cookie", cookie.toString());
            logger.info("Secure session cookie created: name={}, maxAge={}, secure={}, sameSite={}",
                    config.getSessionIdName(), SESSION_TTL_SECONDS, cookieSecure, cookieSameSite);

            response.sendRedirect(personaService.getCallbackUrl(persona));
        } catch (Exception e) {
            logger.error("Authentication failed: {}", e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication failed");
            } catch (Exception ex) {
                logger.error("Failed to send error response", ex);
            }
        }
    }
}


