package com.maityp394.studentapi.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Automatically exports the OpenAPI YAML specification to the project root when the application
 * finishes starting in development/local environments.
 */
@Component
@Profile("!prod")
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "openapi.export.enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiDocsExporter {

  private static final Logger log = LoggerFactory.getLogger(OpenApiDocsExporter.class);

  private final Environment environment;

  /**
   * Constructs an exporter instance with the configured Spring environment.
   *
   * @param environment the Spring environment used to resolve server port and export path
   *     properties
   */
  public OpenApiDocsExporter(Environment environment) {
    this.environment = environment;
  }

  /**
   * Fetches the generated OpenAPI YAML specification from the local server endpoint once the
   * application finishes starting and writes it to the configured export file path.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void exportApiDocs() {
    String port =
        environment.getProperty(
            "local.server.port", environment.getProperty("server.port", "8000"));
    String contextPath = environment.getProperty("server.servlet.context-path", "");
    String exportFileName = environment.getProperty("openapi.export.path", "api-docs.yaml");

    String url = "http://localhost:" + port + contextPath + "/v3/api-docs.yaml";

    try {
      RestClient restClient = RestClient.create();
      String yaml = restClient.get().uri(url).retrieve().body(String.class);

      if (yaml != null && !yaml.isBlank()) {
        Path targetPath = Paths.get(exportFileName);
        Files.writeString(
            targetPath, yaml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("Successfully exported OpenAPI YAML spec to: {}", targetPath.toAbsolutePath());
      }
    } catch (Exception e) {
      log.warn("Could not auto-export OpenAPI spec: {}", e.getMessage());
    }
  }
}
