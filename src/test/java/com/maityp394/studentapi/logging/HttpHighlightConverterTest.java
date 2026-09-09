package com.maityp394.studentapi.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.ansi.Ansi8BitColor;
import org.springframework.boot.ansi.AnsiBackground;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;

class HttpHighlightConverterTest {

  private HttpHighlightConverter converter;
  private AnsiOutput.Enabled originalAnsiEnabled;

  @BeforeEach
  void setUp() {
    originalAnsiEnabled = AnsiOutput.getEnabled();
    AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
    converter = new HttpHighlightConverter();
    converter.start();
  }

  @AfterEach
  void tearDown() {
    AnsiOutput.setEnabled(originalAnsiEnabled);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("httpLogScenarios")
  void shouldFormatHttpLogsWithAppropriateColors(
      String scenario,
      String rawMessage,
      String duration,
      AnsiBackground statusBg,
      AnsiColor statusFg,
      int status,
      AnsiElement methodBg,
      AnsiColor methodFg,
      String centeredMethod) {
    ILoggingEvent event = mock(ILoggingEvent.class);

    String result = converter.transform(event, rawMessage);

    String expectedStatus = AnsiOutput.toString(statusBg, statusFg, String.format(" %3d ", status));
    String expectedDuration =
        AnsiOutput.toString(
            AnsiBackground.BRIGHT_WHITE, AnsiColor.BLACK, String.format(" %8s ", duration));
    String expectedMethod = AnsiOutput.toString(methodBg, methodFg, centeredMethod);
    String expectedPath =
        "\"" + AnsiOutput.toString(AnsiStyle.UNDERLINE, "/api/v1/students") + "\"";

    assertThat(result)
        .contains(expectedStatus)
        .contains(expectedDuration)
        .contains(expectedMethod)
        .contains(expectedPath);
  }

  private static Stream<Arguments> httpLogScenarios() {
    return Stream.of(
        Arguments.of(
            "HTTP 204 GET with Bright Green Status and Navy Method",
            "GET /api/v1/students -> 204 (15 ms)",
            "15 ms",
            AnsiBackground.BRIGHT_GREEN,
            AnsiColor.BLACK,
            204,
            Ansi8BitColor.background(18),
            AnsiColor.BRIGHT_WHITE,
            "  GET   "),
        Arguments.of(
            "HTTP 404 GET with Bright Yellow Status and Navy Method",
            "GET /api/v1/students -> 404 (3 ms)",
            "3 ms",
            AnsiBackground.BRIGHT_YELLOW,
            AnsiColor.BLACK,
            404,
            Ansi8BitColor.background(18),
            AnsiColor.BRIGHT_WHITE,
            "  GET   "),
        Arguments.of(
            "HTTP 500 POST with Red Status and Bright BRIGHT_BLUE Method",
            "POST /api/v1/students -> 500 (22 ms)",
            "22 ms",
            AnsiBackground.RED,
            AnsiColor.WHITE,
            500,
            AnsiBackground.BRIGHT_BLUE,
            AnsiColor.BLACK,
            "  POST  "),
        Arguments.of(
            "HTTP 204 DELETE with Bright Green Status and Red Method",
            "DELETE /api/v1/students -> 204 (8 ms)",
            "8 ms",
            AnsiBackground.BRIGHT_GREEN,
            AnsiColor.BLACK,
            204,
            AnsiBackground.RED,
            AnsiColor.BRIGHT_WHITE,
            " DELETE "),
        Arguments.of(
            "HTTP 204 PUT with Bright Green Status and Orange Method",
            "PUT /api/v1/students -> 204 (12 ms)",
            "12 ms",
            AnsiBackground.BRIGHT_GREEN,
            AnsiColor.BLACK,
            204,
            Ansi8BitColor.background(208),
            AnsiColor.BLACK,
            "  PUT   "),
        Arguments.of(
            "HTTP 204 PATCH with Bright Green Status and Magenta Method",
            "PATCH /api/v1/students -> 204 (10 ms)",
            "10 ms",
            AnsiBackground.BRIGHT_GREEN,
            AnsiColor.BLACK,
            204,
            AnsiBackground.MAGENTA,
            AnsiColor.BRIGHT_WHITE,
            " PATCH  "));
  }

  @Test
  void shouldFormatPlainTextWhenAnsiIsDisabled() {
    AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
    ILoggingEvent event = mock(ILoggingEvent.class);

    String result = converter.transform(event, "GET /api/v1/students -> 204 (15 ms)");

    assertThat(result).isEqualTo("  204  |     15 ms  |   GET    \"/api/v1/students\"");
  }

  @Test
  void shouldLeaveNonHttpLogMessagesUnchanged() {
    ILoggingEvent event = mock(ILoggingEvent.class);
    String standardMessage = "Student created successfully: id=12345";

    String result = converter.transform(event, standardMessage);

    assertThat(result).isEqualTo(standardMessage);
  }

  @Test
  void shouldReturnEmptyStringWhenInputIsNull() {
    ILoggingEvent event = mock(ILoggingEvent.class);

    String result = converter.transform(event, null);

    assertThat(result).isEmpty();
  }
}
