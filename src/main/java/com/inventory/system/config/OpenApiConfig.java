package com.inventory.system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger metadata for the Inventory System API. springdoc scans the
 * controllers and DTOs automatically; this bean only supplies the top-level
 * title, description, and version shown in Swagger UI (/swagger-ui.html).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventorySystemOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Inventory System API")
                .description("REST API for managing products, stock, sales, and reports.")
                .version("v1")
                .contact(new Contact().name("Inventory System")));
    }
}
