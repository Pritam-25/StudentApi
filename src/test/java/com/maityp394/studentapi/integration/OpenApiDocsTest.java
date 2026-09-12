package com.maityp394.studentapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("OpenAPI Documentation Integration Tests")
class OpenApiDocsTest extends BaseIntegrationTest {

  @Test
  @DisplayName(
      "GET /v3/api-docs.yaml should return valid OpenAPI YAML with updated password pattern")
  void shouldReturnValidOpenApiYaml() throws Exception {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/v3/api-docs.yaml", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    String yaml =
        response.getBody().replaceAll("url: http://localhost:\\d+", "url: http://localhost:8000");
    assertThat(yaml)
        .contains("pattern: \"^(?=.*[A-Z])(?=.*\\\\d)(?=.*[^a-zA-Z0-9\\\\s]).{8,30}$\"");

    Path path = Path.of("api-docs.yaml");
    Files.writeString(path, yaml);
  }
}
