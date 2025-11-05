# Swagger Integration Guide

## Overview
Swagger/OpenAPI documentation has been successfully integrated into the Auth Server application using Springdoc OpenAPI.

## Access Points

### Swagger UI
- **URL**: `http://localhost:7000/swagger-ui.html` (Docker)
- **URL**: `http://localhost:8080/swagger-ui.html` (Local)

### OpenAPI JSON
- **URL**: `http://localhost:7000/v3/api-docs` (Docker)
- **URL**: `http://localhost:8080/v3/api-docs` (Local)

## Files Created

### 1. Configuration Files
- **SwaggerConfig.java** - Main Swagger/OpenAPI configuration
  - Located: `src/main/java/com/gms_server/auth_app/configs/`
  - Configures API info, contact details, servers, and license

### 2. Documentation Helpers
- **ApiDocumentation.java** - API tag definitions
  - Located: `src/main/java/com/gms_server/auth_app/swagger/`
  - Contains reusable tags for different API categories

- **ApiResponseExamples.java** - Example responses
  - Located: `src/main/java/com/gms_server/auth_app/swagger/`
  - Predefined response examples for common scenarios

### 3. Properties File
- **swagger-config.properties** - Swagger properties configuration
  - Located: `src/main/resources/`
  - Controls Swagger UI behavior and settings

### 4. Dependencies
- Added Springdoc OpenAPI dependency to `pom.xml`
  - Version: 2.7.0 (Latest stable for Spring Boot 3.x)

## How to Use Swagger Annotations

### Example: Annotating a Controller

```java
package com.gms_server.auth_app.controllers;

import com.gms_server.auth_app.swagger.ApiDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for user login and callback handling")
public class AuthenticationController {

    @GetMapping("/start/{persona}")
    @Operation(
        summary = "Start authentication for a persona",
        description = "Initiates the authentication flow for the specified persona (vendor, consumer, affiliate, gms)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication started successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid persona"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> startPersonaAuthentication(
        @Parameter(description = "Persona type (vendor, consumer, affiliate, gms)", required = true)
        @PathVariable String persona
    ) {
        // Implementation
    }

    @GetMapping("/callback")
    @Operation(
        summary = "Handle authentication callback",
        description = "Processes the OAuth2 callback after user authentication"
    )
    public void callbackAuthentication(
        @Parameter(description = "Authorization code from OAuth2 provider")
        @RequestParam(required = false) String code,
        
        @Parameter(description = "State parameter for CSRF protection")
        @RequestParam(required = false) String state,
        
        @Parameter(description = "Error message if authentication failed")
        @RequestParam(required = false) String error,
        
        HttpServletResponse response
    ) {
        // Implementation
    }
}
```

## Common Annotations

### Controller Level
- `@Tag(name = "Category", description = "Description")` - Groups endpoints

### Method Level
- `@Operation(summary = "...", description = "...")` - Describes the endpoint
- `@ApiResponses({...})` - Lists possible responses
- `@ApiResponse(responseCode = "200", description = "...")` - Individual response

### Parameter Level
- `@Parameter(description = "...", required = true/false)` - Describes parameters

## Configuration Options

### Enable/Disable Swagger
In `application.properties` or `application.yml`:
```properties
springdoc.swagger-ui.enabled=true
springdoc.api-docs.enabled=true
```

### Customize Paths
```properties
springdoc.swagger-ui.path=/api-docs
springdoc.api-docs.path=/v3/api-docs
```

### Security (Production)
To disable Swagger in production:
```properties
# In application-prod.properties
springdoc.swagger-ui.enabled=false
springdoc.api-docs.enabled=false
```

## Building and Running

### Local Development
```bash
mvn clean install
mvn spring-boot:run
```
Then visit: `http://localhost:8080/swagger-ui.html`

### Docker
```bash
docker-compose up --build
```
Then visit: `http://localhost:7000/swagger-ui.html`

## Features Enabled

✅ Interactive API documentation
✅ Try-it-out functionality
✅ Request/response examples
✅ Schema definitions
✅ Filtering and searching
✅ Request duration display
✅ Multiple server configurations
✅ Sorted operations and tags

## Troubleshooting

### Swagger UI not loading
- Check if application is running
- Verify port configuration
- Check `springdoc.swagger-ui.enabled=true` in properties

### Endpoints not showing
- Verify controller has `@RestController` annotation
- Check `springdoc.packages-to-scan` includes your package
- Ensure endpoints match `springdoc.paths-to-match` pattern

### 404 Error
- Make sure you're using the correct path: `/swagger-ui.html`
- Check application logs for errors

## Next Steps

1. Add `@Tag` annotations to all controllers
2. Add `@Operation` annotations to all endpoints
3. Add `@Parameter` descriptions to all parameters
4. Define response models with `@Schema` annotations
5. Add security schemes if needed
6. Customize API info in `SwaggerConfig.java`

## References

- [Springdoc OpenAPI Documentation](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Swagger Annotations Guide](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)

