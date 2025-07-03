package com.exemple.testotp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Keycloak OTP Registration API")
                        .version("1.0")
                        .description("API pour l'inscription avec vérification OTP par SMS")
                        .contact(new Contact()
                                .name("Support")
                                .email("support@example.com")));
    }
}
