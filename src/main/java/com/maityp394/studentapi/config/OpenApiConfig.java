package com.maityp394.studentapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 configuration for the Student Management API.
 *
 * <p>Configures global API metadata and security schemes for both Bearer JWT and HttpOnly cookie
 * authentication, and customizes anonymous endpoints to explicitly emit {@code security: - {}}.
 */
@Configuration
public class OpenApiConfig {

  public static final String BEARER_AUTH = "bearerAuth";
  public static final String COOKIE_AUTH = "cookieAuth";
  public static final String REFRESH_COOKIE_AUTH = "refreshCookieAuth";

  private static final String LOGIN_PATH = "/api/v1/auth/login";
  private static final String REGISTER_PATH = "/api/v1/auth/register";
  private static final String LOGOUT_PATH = "/api/v1/auth/logout";

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
                        .description("HttpOnly access_token cookie set upon login."))
                .addSecuritySchemes(
                    REFRESH_COOKIE_AUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("refresh_token")
                        .description("HttpOnly refresh_token cookie used to renew sessions.")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
        .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH));
  }

  @Bean
  OpenApiCustomizer anonymousAuthEndpointsCustomizer() {
    return openApi -> {
      if (openApi.getPaths() == null) {
        return;
      }
      List.of(LOGIN_PATH, REGISTER_PATH, LOGOUT_PATH)
          .forEach(
              path -> {
                PathItem pathItem = openApi.getPaths().get(path);
                if (pathItem != null && pathItem.getPost() != null) {
                  pathItem.getPost().setSecurity(List.of(new SecurityRequirement()));
                }
              });
    };
  }
}
