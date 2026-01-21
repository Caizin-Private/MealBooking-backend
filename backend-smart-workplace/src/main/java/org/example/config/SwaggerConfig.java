package org.example.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI smartWorkplaceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Workplace Booking API")
                        .description("Backend APIs for Smart Workplace system")
                        .version("1.0.0"));
    }
}
