package com.gms_server.auth_app.controllers;

import com.gms_server.auth_app.services.AuthenticationService;
import com.gms_server.auth_app.services.PersonaService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    private final AuthenticationService authenticationService;
    private final PersonaService personaService;

    public AuthenticationController(AuthenticationService authenticationService, PersonaService personaService) {
        this.authenticationService = authenticationService;
        this.personaService = personaService;
    }

    @GetMapping("/start/{persona}")
    public ResponseEntity<Void> startPersonaAuthentication(@PathVariable String persona) {
        logger.info("controller-startPersonaAuthentication start.......");
        if (!personaService.isValidPersona(persona)) {
            logger.error("Invalid persona: {}", persona);
            return ResponseEntity.badRequest().build();
        }
        logger.info("persona: {}", persona);
        return authenticationService.startAuthentication(persona);
    }

    @GetMapping("/callback")
    public void callbackAuthentication(@RequestParam(required = false) String code, @RequestParam(required = false) String state, @RequestParam(required = false) String error, HttpServletResponse response) {
        logger.info("controller-callbackAuthentication start.......");
        authenticationService.handleCallback(code, state, error, response);
    }
}
