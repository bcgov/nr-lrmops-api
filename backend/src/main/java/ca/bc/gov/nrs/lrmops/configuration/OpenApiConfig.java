package ca.bc.gov.nrs.lrmops.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration. Declares a bearer-token (JWT) security scheme so the
 * Swagger UI "Authorize" dialog accepts a Keycloak access token. Controllers reference it with
 * {@code @SecurityRequirement(name = "bearerAuth")}.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "LRM Ops API",
        version = "1.0",
        description = "Read-only access to LRMOPS road data in TFM. All endpoints require a "
            + "Keycloak bearer token carrying the relevant scope."))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiConfig {
}
