package com.gms_server.auth_app.swagger;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API Documentation Tags
 * Define reusable tags for API endpoints
 */
public class ApiDocumentation {

    @Tag(name = "Authentication", description = "Authentication endpoints for user login and callback handling")
    public interface AuthenticationAPI {}

    @Tag(name = "Authorization", description = "Authorization endpoints for validating tokens and sessions")
    public interface AuthorizationAPI {}

    @Tag(name = "Idempotency", description = "Idempotency endpoints for ensuring request uniqueness")
    public interface IdempotencyAPI {}

    @Tag(name = "Health", description = "Health check and monitoring endpoints")
    public interface HealthAPI {}
}

