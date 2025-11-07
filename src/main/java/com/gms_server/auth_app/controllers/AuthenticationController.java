package com.gms_server.auth_app.controllers;

import com.gms_server.auth_app.services.AuthenticationService;
import com.gms_server.auth_app.services.PersonaService;
import com.gms_server.auth_app.utils.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ============================================
 * AUTHENTICATION CONTROLLER
 * ============================================
 * 
 * Handles OAuth 2.0 / OIDC authentication flow:
 * 1. Start authentication (/start/{persona})
 * 2. Handle callback (/callback)
 * 3. Logout (/logout/{persona})
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    private final AuthenticationService authenticationService;
    private final PersonaService personaService;
    private final ValidationUtils validationUtils;

    public AuthenticationController(
            AuthenticationService authenticationService, 
            PersonaService personaService,
            ValidationUtils validationUtils) {
        this.authenticationService = authenticationService;
        this.personaService = personaService;
        this.validationUtils = validationUtils;
    }

    /**
     * Start authentication flow for a persona
     * 
     * @param persona The persona to authenticate (vendor, consumer, affiliate, gms)
     * @return Redirect to OAuth provider
     */
    @GetMapping("/start/{persona}")
    public ResponseEntity<Void> startPersonaAuthentication(@PathVariable String persona) {
        logger.info("🎬 [CONTROLLER] Authentication start request for persona: {}", persona);
        
        // Input validation
        if (persona == null || persona.trim().isEmpty()) {
            logger.error("❌ [CONTROLLER] Empty persona provided");
            return ResponseEntity.badRequest().build();
        }
        
        // Sanitize input
        persona = validationUtils.sanitize(persona);
        
        // Validate persona format
        if (!validationUtils.isValidPersona(persona)) {
            logger.error("❌ [CONTROLLER] Invalid persona format: {}", persona);
            return ResponseEntity.badRequest().build();
        }
        
        // Validate persona is supported
        if (!personaService.isValidPersona(persona)) {
            logger.error("❌ [CONTROLLER] Unsupported persona: {}", persona);
            return ResponseEntity.badRequest().build();
        }
        
        logger.info("✅ [CONTROLLER] Valid persona: {}", persona);
        return authenticationService.startAuthentication(persona);
    }

    /**
     * Handle OAuth callback from provider
     * 
     * @param code Authorization code from OAuth provider
     * @param state State parameter for CSRF protection
     * @param error Error from OAuth provider (if any)
     * @param response HTTP response for redirect
     */
    @GetMapping("/callback")
    public void callbackAuthentication(
            @RequestParam(required = false) String code, 
            @RequestParam(required = false) String state, 
            @RequestParam(required = false) String error, 
            HttpServletResponse response) {
        
        logger.info("📞 [CONTROLLER] OAuth callback received");
        
        // Input validation for state parameter
        if (state != null && !validationUtils.isValidState(state)) {
            logger.error("❌ [CONTROLLER] Invalid state format");
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid state parameter format");
            } catch (Exception e) {
                logger.error("❌ [CONTROLLER] Failed to send error response", e);
            }
            return;
        }
        
        // Input validation for authorization code
        if (code != null && !validationUtils.isValidAuthCode(code)) {
            logger.error("❌ [CONTROLLER] Invalid authorization code format");
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid authorization code format");
            } catch (Exception e) {
                logger.error("❌ [CONTROLLER] Failed to send error response", e);
            }
            return;
        }
        
        authenticationService.handleCallback(code, state, error, response);
    }

    /**
     * Logout user and clear session
     * 
     * @param persona The persona to logout from
     * @param request HTTP request to get cookies
     * @param response HTTP response to clear cookies
     * @return Logout result with redirect URI
     */
    @PostMapping("/logout/{persona}")
    public ResponseEntity<Map<String, String>> logout(
            @PathVariable String persona,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        logger.info("🚪 [CONTROLLER] Logout request for persona: {}", persona);
        
        // Input validation
        if (persona == null || persona.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Persona is required"));
        }
        
        // Sanitize input
        persona = validationUtils.sanitize(persona);
        
        // Validate persona format
        if (!validationUtils.isValidPersona(persona)) {
            logger.error("❌ [CONTROLLER] Invalid persona format: {}", persona);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid persona format"));
        }
        
        // Validate persona is supported
        if (!personaService.isValidPersona(persona)) {
            logger.error("❌ [CONTROLLER] Unsupported persona: {}", persona);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Unsupported persona"));
        }
        
        Map<String, String> result = authenticationService.logout(persona, request, response);
        
        if (result.containsKey("error")) {
            return ResponseEntity.badRequest().body(result);
        }
        
        return ResponseEntity.ok(result);
    }
}
