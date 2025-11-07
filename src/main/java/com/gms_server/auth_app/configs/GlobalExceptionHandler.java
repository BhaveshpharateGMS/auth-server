package com.gms_server.auth_app.configs;

import com.gms_server.auth_app.services.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * ============================================
 * GLOBAL EXCEPTION HANDLER
 * ============================================
 * 
 * Centralized exception handling for consistent error responses
 * Prevents internal error details from leaking to clients
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle custom authorization exceptions
     */
    @ExceptionHandler(AuthorizationService.AuthorizationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthorizationException(
            AuthorizationService.AuthorizationException ex) {
        logger.error("❌ [EXCEPTION] Authorization error: {}", ex.getMessage());
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(Map.of(
                        "error", "Authorization failed",
                        "message", ex.getMessage(),
                        "status", ex.getStatusCode()
                ));
    }

    /**
     * Handle illegal argument exceptions (validation failures)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        logger.error("❌ [EXCEPTION] Invalid argument: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "error", "Invalid request",
                        "message", "The provided parameters are invalid"
                ));
    }

    /**
     * Handle missing required parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleMissingParams(
            MissingServletRequestParameterException ex) {
        logger.error("❌ [EXCEPTION] Missing parameter: {}", ex.getParameterName());
        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "error", "Missing parameter",
                        "message", "Required parameter '" + ex.getParameterName() + "' is missing",
                        "parameter", ex.getParameterName()
                ));
    }

    /**
     * Handle type mismatch errors
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        logger.error("❌ [EXCEPTION] Type mismatch for parameter: {}", ex.getName());
        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "error", "Invalid parameter type",
                        "message", "Parameter '" + ex.getName() + "' has invalid format",
                        "parameter", ex.getName()
                ));
    }

    /**
     * Handle null pointer exceptions
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, String>> handleNullPointerException(NullPointerException ex) {
        logger.error("❌ [EXCEPTION] Null pointer exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal server error",
                        "message", "An unexpected error occurred. Please try again later."
                ));
    }

    /**
     * Handle all other unexpected exceptions
     * SECURITY: Don't expose internal error details to clients
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        // Log full exception details internally
        logger.error("❌ [EXCEPTION] Unexpected error:", ex);
        
        // Return generic error message to client (don't expose internals)
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal server error",
                        "message", "An unexpected error occurred. Please try again later."
                ));
    }
}

