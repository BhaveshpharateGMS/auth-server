package com.gms_server.auth_app.swagger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Example Controller showing Swagger annotations usage
 * This is a reference implementation - you can delete this file if not needed
 */
@RestController
@RequestMapping("/api/v1/example")
@Tag(name = "Example", description = "Example endpoints showing Swagger annotations usage (DELETE THIS CONTROLLER IN PRODUCTION)")
public class SwaggerExampleController {

    @GetMapping("/health")
    @Operation(
            summary = "Health check endpoint",
            description = "Returns the health status of the service"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Service is healthy",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = ApiResponseExamples.SUCCESS_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Service is unhealthy",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = ApiResponseExamples.ERROR_RESPONSE)
                    )
            )
    })
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "auth-server"
        ));
    }

    @GetMapping("/persona/{type}")
    @Operation(
            summary = "Get persona information",
            description = "Retrieves information about a specific persona type"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Persona information retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid persona type",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = ApiResponseExamples.BAD_REQUEST_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = ApiResponseExamples.UNAUTHORIZED_RESPONSE)
                    )
            )
    })
    public ResponseEntity<Map<String, String>> getPersonaInfo(
            @Parameter(
                    description = "Type of persona (vendor, consumer, affiliate, gms)",
                    required = true,
                    example = "vendor"
            )
            @PathVariable String type
    ) {
        // Validate persona type
        if (!isValidPersona(type)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid persona type",
                    "allowed", "vendor, consumer, affiliate, gms"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "type", type,
                "status", "active",
                "description", "Persona information for " + type
        ));
    }

    @PostMapping("/validate")
    @Operation(
            summary = "Validate request",
            description = "Validates a request with given parameters"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Validation successful"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = ApiResponseExamples.VALIDATION_ERROR_RESPONSE)
                    )
            )
    })
    public ResponseEntity<Map<String, Object>> validateRequest(
            @Parameter(
                    description = "Request ID for validation",
                    required = true
            )
            @RequestParam String requestId,

            @Parameter(
                    description = "Session token",
                    required = false
            )
            @RequestParam(required = false) String sessionToken,

            @Parameter(
                    description = "Request body containing validation data",
                    required = true
            )
            @RequestBody(required = false) Map<String, Object> body
    ) {
        return ResponseEntity.ok(Map.of(
                "requestId", requestId,
                "valid", true,
                "timestamp", System.currentTimeMillis()
        ));
    }

    // Helper method
    private boolean isValidPersona(String type) {
        return type != null && (
                type.equalsIgnoreCase("vendor") ||
                        type.equalsIgnoreCase("consumer") ||
                        type.equalsIgnoreCase("affiliate") ||
                        type.equalsIgnoreCase("gms")
        );
    }
}

