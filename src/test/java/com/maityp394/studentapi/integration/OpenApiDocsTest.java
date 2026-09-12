package com.maityp394.studentapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("OpenAPI Documentation Integration Tests")
class OpenApiDocsTest extends BaseIntegrationTest {

  private final YAMLMapper yamlMapper = new YAMLMapper();

  @Test
  @DisplayName("GET /v3/api-docs.yaml should match the committed api-docs.yaml specification")
  void shouldMatchCommittedOpenApiYaml() throws Exception {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/v3/api-docs.yaml", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    String generatedYaml =
        response.getBody().replaceAll("url: http://localhost:\\d+", "url: http://localhost:8000");

    Path path = Path.of("api-docs.yaml");
    assertThat(path).as("api-docs.yaml file must exist at project root").exists();

    String committedYaml = Files.readString(path);

    JsonNode generatedNode = yamlMapper.readTree(generatedYaml);
    JsonNode committedNode = yamlMapper.readTree(committedYaml);

    assertThat(generatedNode)
        .as(
            "The generated OpenAPI specification does not match the committed api-docs.yaml. "
                + "Ensure api-docs.yaml is synchronized with current endpoints and schemas.")
        .isEqualTo(committedNode);
  }

  @Test
  @DisplayName(
      "Security requirements should correctly configure anonymous, scheme-specific, and inherited operations")
  void shouldConfigureSemanticSecurityRequirementsCorrectly() throws Exception {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/v3/api-docs.yaml", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    JsonNode openApiNode = yamlMapper.readTree(response.getBody());

    // 1. Global security requirements: bearerAuth OR cookieAuth
    JsonNode globalSecurity = openApiNode.path("security");
    assertThat(globalSecurity.isArray()).isTrue();
    assertThat(globalSecurity).hasSize(2);
    assertThat(globalSecurity.get(0).has("bearerAuth")).isTrue();
    assertThat(globalSecurity.get(1).has("cookieAuth")).isTrue();

    // 2. Anonymous endpoints must explicitly declare security: [{}]
    List<String> anonymousPaths =
        List.of("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/logout");
    for (String path : anonymousPaths) {
      JsonNode securityNode = openApiNode.path("paths").path(path).path("post").path("security");
      assertThat(securityNode.isArray())
          .as("Path %s should define a security array", path)
          .isTrue();
      assertThat(securityNode)
          .as("Path %s should have exactly one security requirement entry (empty object)", path)
          .hasSize(1);
      assertThat(securityNode.get(0).isObject())
          .as("Path %s security item should be an object", path)
          .isTrue();
      assertThat(securityNode.get(0).isEmpty())
          .as("Path %s should have empty security object indicating anonymous access", path)
          .isTrue();
    }

    // 3. /api/v1/auth/refresh must require refreshCookieAuth only
    JsonNode refreshSecurity =
        openApiNode.path("paths").path("/api/v1/auth/refresh").path("post").path("security");
    assertThat(refreshSecurity.isArray()).isTrue();
    assertThat(refreshSecurity).hasSize(1);
    assertThat(refreshSecurity.get(0).has("refreshCookieAuth")).isTrue();

    // 4. Authenticated endpoints must not override security, thereby inheriting global security
    JsonNode meSecurity =
        openApiNode.path("paths").path("/api/v1/auth/me").path("get").path("security");
    assertThat(meSecurity.isMissingNode())
        .as("/api/v1/auth/me should not override security, inheriting global security")
        .isTrue();

    JsonNode logoutAllSecurity =
        openApiNode.path("paths").path("/api/v1/auth/logout-all").path("post").path("security");
    assertThat(logoutAllSecurity.isMissingNode())
        .as("/api/v1/auth/logout-all should not override security, inheriting global security")
        .isTrue();

    JsonNode studentsSecurity =
        openApiNode.path("paths").path("/api/v1/students").path("get").path("security");
    assertThat(studentsSecurity.isMissingNode())
        .as("/api/v1/students should not override security, inheriting global security")
        .isTrue();
  }
}
