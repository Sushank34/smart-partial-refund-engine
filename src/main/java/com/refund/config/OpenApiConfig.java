package com.refund.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI metadata, served at /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI partialRefundOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Smart Partial Refund Engine")
                .version("1.0.0")
                .description("Proportional, multi-currency, fully audited partial refunds."));
    }
}
