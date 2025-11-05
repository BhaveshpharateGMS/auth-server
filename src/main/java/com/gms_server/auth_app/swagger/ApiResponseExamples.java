package com.gms_server.auth_app.swagger;

/**
 * API Response Examples
 * Contains example responses for Swagger documentation
 */
public class ApiResponseExamples {

    public static final String SUCCESS_RESPONSE = """
            {
              "status": "success",
              "message": "Operation completed successfully"
            }
            """;

    public static final String ERROR_RESPONSE = """
            {
              "status": "error",
              "message": "An error occurred",
              "timestamp": "2025-11-05T10:30:00Z"
            }
            """;

    public static final String UNAUTHORIZED_RESPONSE = """
            {
              "status": "error",
              "message": "Unauthorized access",
              "code": 401
            }
            """;

    public static final String BAD_REQUEST_RESPONSE = """
            {
              "status": "error",
              "message": "Invalid request parameters",
              "code": 400
            }
            """;

    public static final String VALIDATION_ERROR_RESPONSE = """
            {
              "status": "error",
              "message": "Validation failed",
              "errors": [
                "Invalid persona type",
                "Missing required parameters"
              ]
            }
            """;
}

