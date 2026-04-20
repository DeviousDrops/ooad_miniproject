package com.pharmacy.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pharmacyOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pharmacy Inventory & Billing API")
                        .version("1.0.0")
                        .description("Spring Boot MVC + Security + JPA API documentation"));
    }
}
