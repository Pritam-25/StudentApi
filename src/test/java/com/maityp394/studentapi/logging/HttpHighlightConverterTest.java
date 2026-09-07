package com.maityp394.studentapi.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HttpHighlightConverterTest {

  private HttpHighlightConverter converter;

  @BeforeEach
  void setUp() {
    converter = new HttpHighlightConverter();
    converter.start();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("httpLogScenarios")
  void shouldFormatHttpLogsWithAppropriateColors(
      String scenario, String rawMessage, String expectedStatusAnsi, String expectedMethodAnsi) {
    ILoggingEvent event = mock(ILoggingEvent.class);

    String result = converter.transform(event, rawMessage);

    assertThat(result)
        .contains(
            expectedStatusAnsi,
            expectedMethodAnsi,
            "\u001B[47m\u001B[30m", // White BG for duration
            "\u001B[4m/api/v1/students\u001B[24m"); // Underlined path
  }

  private static Stream<Arguments> httpLogScenarios() {
    return Stream.of(
        Arguments.of(
            "HTTP 200 GET with Green Status and Navy Method",
            "GET /api/v1/students -> 200 (15 ms)",
            "\u001B[42m\u001B[30m",
            "\u001B[48;5;18m\u001B[37m  GET   \u001B[0m"),
        Arguments.of(
            "HTTP 404 GET with Yellow Status and Navy Method",
            "GET /api/v1/students -> 404 (3 ms)",
            "\u001B[43m\u001B[30m",
            "\u001B[48;5;18m\u001B[37m  GET   \u001B[0m"),
        Arguments.of(
            "HTTP 500 POST with Red Status and Blue Method",
            "POST /api/v1/students -> 500 (22 ms)",
            "\u001B[41m\u001B[37m",
            "\u001B[44m\u001B[37m  POST  \u001B[0m"),
        Arguments.of(
            "HTTP 204 DELETE with Green Status and Red Method",
            "DELETE /api/v1/students -> 204 (8 ms)",
            "\u001B[42m\u001B[30m",
            "\u001B[41m\u001B[37m DELETE \u001B[0m"),
        Arguments.of(
            "HTTP 200 PUT with Green Status and Orange Method",
            "PUT /api/v1/students -> 200 (12 ms)",
            "\u001B[42m\u001B[30m",
            "\u001B[48;5;214m\u001B[30m  PUT   \u001B[0m"),
        Arguments.of(
            "HTTP 200 PATCH with Green Status and Magenta Method",
            "PATCH /api/v1/students -> 200 (10 ms)",
            "\u001B[42m\u001B[30m",
            "\u001B[45m\u001B[37m PATCH  \u001B[0m"));
  }

  @Test
  void shouldLeaveNonHttpLogMessagesUnchanged() {
    ILoggingEvent event = mock(ILoggingEvent.class);
    String standardMessage = "Student created successfully: id=12345";

    String result = converter.transform(event, standardMessage);

    assertThat(result).isEqualTo(standardMessage);
  }
}
