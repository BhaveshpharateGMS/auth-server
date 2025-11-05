package com.gms_server.auth_app.utils;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class PkceService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64UrlEncoder = Base64.getUrlEncoder().withoutPadding();

    // Generate code_verifier (RFC 7636: 43–128 chars)
    public static String generateCodeVerifier() {
        byte[] code = new byte[64];
        secureRandom.nextBytes(code);
        return base64UrlEncoder.encodeToString(code);
    }

    // Generate code_challenge from code_verifier
    public static String generateCodeChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return base64UrlEncoder.encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }

    // Generate state (UUID is fine, or random bytes)
    public static String generateState() {
        return UUID.randomUUID().toString();
    }

    public static String generateSessionId() {
        return UUID.randomUUID().toString();
    }
}