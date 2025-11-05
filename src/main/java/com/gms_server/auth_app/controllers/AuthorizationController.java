package com.gms_server.auth_app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gms_server.auth_app.services.AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/verify")
public class AuthorizationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationController.class);

    private final AuthorizationService authorizationService;
    private final ObjectMapper objectMapper;

    public AuthorizationController(AuthorizationService authorizationService, ObjectMapper objectMapper) {
        this.authorizationService = authorizationService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{persona}")
    public ResponseEntity<Map<String, Object>> verifyPersona(
            @PathVariable String persona,
            HttpServletRequest request) {

        logger.info("Received verification request for persona: {}", persona);

        try {
            Map<String, Object> userInfo = authorizationService.verifyPersonaAuthorization(persona, request);

            // Extract user information
            String userId = userInfo.get("sub") != null ? userInfo.get("sub").toString() : "";
            String userEmail = userInfo.get("email") != null ? userInfo.get("email").toString() : "";
            String userInfoJson = objectMapper.writeValueAsString(userInfo);

            // Create response headers for nginx
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", userId);
            headers.set("X-User-Email", userEmail);
            headers.set("X-User-Info", userInfoJson);

            logger.info("Authorization successful for persona: {}, userId: {}", persona, userId);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(Map.of(
                            "success", true,
                            "persona", persona,
                            "userId", userId,
                            "email", userEmail
                    ));

        } catch (AuthorizationService.AuthorizationException e) {
            logger.error("Authorization failed for persona {}: {}", persona, e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            logger.error("Unexpected error during authorization for persona: {}", persona, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authorization failed"));
        }
    }
}

