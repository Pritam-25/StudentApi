package com.maityp394.studentapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 configuration for the Student Management API.
 *
 * <p>Configures global API metadata and security schemes for both Bearer JWT and HttpOnly cookie
 * authentication.
 */
@Configuration
public class OpenApiConfig {

  public static final String BEARER_AUTH = "bearerAuth";
  public static final String COOKIE_AUTH = "cookieAuth";

  @Bean
  OpenAPI studentOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Student Management API")
                .version("1.0.0")
                .description(
                    "Production-grade Student Management REST API with JWT and cookie authentication.")
                .contact(
                    new Contact()
                        .name("Pritam Maity")
                        .url("https://github.com/Pritam-25/StudentApi"))
                .license(new License().name("MIT").identifier("MIT")))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter your JWT token directly."))
                .addSecuritySchemes(
                    COOKIE_AUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("access_token")
                        .description("HttpOnly access_token cookie set upon login.")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
        .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH));
  }
}
