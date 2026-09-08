package com.maityp394.studentapi.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.maityp394.studentapi.config.properties.JwtProperties;
import com.maityp394.studentapi.config.properties.SecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

class PropertiesValidationTest {

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties({JwtProperties.class, SecurityProperties.class})
  static class TestConfig {}

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  ConfigurationPropertiesAutoConfiguration.class,
                  ValidationAutoConfiguration.class))
          .withUserConfiguration(TestConfig.class);

  @Test
  @DisplayName("Should successfully bind valid configuration properties")
  void shouldBindValidConfiguration() {
    contextRunner
        .withPropertyValues(
            "jwt.issuer=https://auth.example.com",
            "jwt.secret=this-is-a-valid-secret-key-with-at-least-32-chars",
            "jwt.expiration-ms=60000",
            "app.security.cookie.secure=true")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              JwtProperties jwtProperties = context.getBean(JwtProperties.class);
              assertThat(jwtProperties.issuer()).isEqualTo("https://auth.example.com");
              assertThat(jwtProperties.secret())
                  .isEqualTo("this-is-a-valid-secret-key-with-at-least-32-chars");
              assertThat(jwtProperties.expirationMs()).isEqualTo(60000L);

              SecurityProperties securityProperties = context.getBean(SecurityProperties.class);
              assertThat(securityProperties.cookie().secure()).isTrue();
            });
  }

  @Test
  @DisplayName("Should apply defaults when optional properties are omitted")
  void shouldApplyDefaultValues() {
    contextRunner
        .withPropertyValues("jwt.secret=this-is-a-valid-secret-key-with-at-least-32-chars")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              JwtProperties jwtProperties = context.getBean(JwtProperties.class);
              assertThat(jwtProperties.issuer()).isEqualTo("api.maityp394.live");
              assertThat(jwtProperties.expirationMs()).isEqualTo(900000L);

              SecurityProperties securityProperties = context.getBean(SecurityProperties.class);
              assertThat(securityProperties.cookie()).isNotNull();
              assertThat(securityProperties.cookie().secure()).isFalse();
            });
  }

  @Test
  @DisplayName("Should fail startup when jwt.secret is missing or blank")
  void shouldFailWhenJwtSecretIsBlank() {
    contextRunner
        .withPropertyValues("jwt.secret=")
        .run(
            context -> {
              assertThat(context.getStartupFailure()).isNotNull();
              assertThat(context.getStartupFailure()).hasStackTraceContaining("must not be blank");
            });
  }

  @Test
  @DisplayName("Should fail startup when jwt.secret is fewer than 32 characters")
  void shouldFailWhenJwtSecretIsTooShort() {
    contextRunner
        .withPropertyValues("jwt.secret=short-key-under-32-chars")
        .run(
            context -> {
              assertThat(context.getStartupFailure()).isNotNull();
              assertThat(context.getStartupFailure())
                  .hasStackTraceContaining("must be at least 32 characters");
            });
  }

  @Test
  @DisplayName("Should fail startup when jwt.expiration-ms is zero or negative")
  void shouldFailWhenExpirationIsNotPositive() {
    contextRunner
        .withPropertyValues(
            "jwt.secret=this-is-a-valid-secret-key-with-at-least-32-chars", "jwt.expiration-ms=0")
        .run(
            context -> {
              assertThat(context.getStartupFailure()).isNotNull();
              assertThat(context.getStartupFailure()).hasStackTraceContaining("greater than 0");
            });

    contextRunner
        .withPropertyValues(
            "jwt.secret=this-is-a-valid-secret-key-with-at-least-32-chars",
            "jwt.expiration-ms=-100")
        .run(
            context -> {
              assertThat(context.getStartupFailure()).isNotNull();
              assertThat(context.getStartupFailure()).hasStackTraceContaining("greater than 0");
            });
  }

  @Test
  @DisplayName(
      "Should bind relaxed environment variables (JWT_SECRET, JWT_ISSUER, APP_SECURITY_COOKIE_SECURE)")
  void shouldBindRelaxedEnvironmentVariables() {
    contextRunner
        .withInitializer(
            context ->
                org.springframework.boot.test.util.TestPropertyValues.of(
                        "JWT_SECRET=this-is-a-valid-secret-key-with-at-least-32-chars",
                        "JWT_ISSUER=api.custom-issuer.com",
                        "JWT_EXPIRATION_MS=120000",
                        "APP_SECURITY_COOKIE_SECURE=true")
                    .applyTo(
                        context.getEnvironment(),
                        org.springframework.boot.test.util.TestPropertyValues.Type
                            .SYSTEM_ENVIRONMENT,
                        "test-systemEnvironment"))
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              JwtProperties jwtProperties = context.getBean(JwtProperties.class);
              assertThat(jwtProperties.secret())
                  .isEqualTo("this-is-a-valid-secret-key-with-at-least-32-chars");
              assertThat(jwtProperties.issuer()).isEqualTo("api.custom-issuer.com");
              assertThat(jwtProperties.expirationMs()).isEqualTo(120000L);

              SecurityProperties securityProperties = context.getBean(SecurityProperties.class);
              assertThat(securityProperties.cookie().secure()).isTrue();
            });
  }
}
