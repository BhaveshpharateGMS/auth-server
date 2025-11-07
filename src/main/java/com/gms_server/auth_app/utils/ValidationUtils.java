package com.gms_server.auth_app.utils;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * ============================================
 * VALIDATION UTILITY
 * ============================================
 * 
 * Centralized input validation and sanitization
 * Prevents injection attacks and ensures data integrity
 */
@Component
public class ValidationUtils {

    // Regex patterns for validation
    private static final Pattern PERSONA_PATTERN = Pattern.compile("^[a-z]{3,20}$");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Validates persona name
     * Must be lowercase letters only, 3-20 characters
     */
    public boolean isValidPersona(String persona) {
        return persona != null && PERSONA_PATTERN.matcher(persona.toLowerCase()).matches();
    }

    /**
     * Validates UUID format (for session IDs, state, etc.)
     */
    public boolean isValidUUID(String uuid) {
        return uuid != null && UUID_PATTERN.matcher(uuid.toLowerCase()).matches();
    }

    /**
     * Validates session ID (must be UUID format)
     */
    public boolean isValidSessionId(String sessionId) {
        return isValidUUID(sessionId);
    }

    /**
     * Validates state parameter (must be UUID format)
     */
    public boolean isValidState(String state) {
        return isValidUUID(state);
    }

    /**
     * Validates authorization code format
     * Must be alphanumeric with underscores/hyphens, 20-512 characters
     */
    public boolean isValidAuthCode(String code) {
        return code != null && 
               code.length() >= 20 && 
               code.length() <= 512 &&
               ALPHANUMERIC_PATTERN.matcher(code).matches();
    }

    /**
     * Sanitizes string input to prevent XSS attacks
     * Removes potentially dangerous characters
     */
    public String sanitize(String input) {
        if (input == null) return null;
        
        // Remove potential XSS patterns
        return input.replaceAll("[<>\"'`]", "")
                   .trim();
    }

    /**
     * Validates email format
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validates that a string is not null, empty, or only whitespace
     */
    public boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Validates string length
     */
    public boolean isValidLength(String str, int minLength, int maxLength) {
        if (str == null) return false;
        int length = str.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Validates that a string contains only alphanumeric characters
     */
    public boolean isAlphanumeric(String str) {
        return str != null && ALPHANUMERIC_PATTERN.matcher(str).matches();
    }
}

