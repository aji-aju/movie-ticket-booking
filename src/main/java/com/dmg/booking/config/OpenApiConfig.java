package com.dmg.booking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI movieBookingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Movie Ticket Booking API")
                        .version("v1")
                        .description("""
                                Seat-level movie ticket booking: public catalog, time-bound seat holds
                                (atomic CAS), booking with pessimistic locking + idempotency, and RBAC.

                                Click **Authorize** and sign in with HTTP Basic:
                                  - alice / password  (CUSTOMER — hold & book)
                                  - admin / password  (ADMIN)
                                """))
                // Registers the "Authorize" button for HTTP Basic auth.
                .components(new Components().addSecuritySchemes(BASIC_AUTH,
                        new SecurityScheme()
                                .name(BASIC_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
    }
}
