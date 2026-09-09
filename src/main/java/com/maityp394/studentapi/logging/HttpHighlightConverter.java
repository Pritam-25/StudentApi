package com.maityp394.studentapi.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.ansi.Ansi8BitColor;
import org.springframework.boot.ansi.AnsiBackground;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;

/**
 * Logback composite conversion rule dedicated to styling HTTP access log messages emitted by {@code
 * RequestLoggingFilter} in development mode.
 *
 * <p>Uses Spring Boot's {@link AnsiOutput}, {@link AnsiBackground}, {@link AnsiColor}, and {@link
 * Ansi8BitColor} to render bright, high-contrast HTTP access logs. When ANSI output is disabled,
 * styles are cleanly stripped without raw escape codes.
 */
public class HttpHighlightConverter extends CompositeConverter<ILoggingEvent> {

  private static final Pattern HTTP_PATTERN =
      Pattern.compile(
          "^(GET|POST|PUT|PATCH|DELETE)\\s+(\\S+)\\s+->\\s+(\\d{3})\\s+\\((\\d+\\s*ms)\\)$");

  private static final int METHOD_WIDTH = 8;

  @Override
  public void start() {
    if (AnsiOutput.getEnabled() == AnsiOutput.Enabled.DETECT) {
      AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
    }
    super.start();
  }

  @Override
  protected String transform(ILoggingEvent event, String in) {
    if (in == null) {
      return "";
    }

    Matcher matcher = HTTP_PATTERN.matcher(in);
    if (!matcher.matches()) {
      return in;
    }

    String method = matcher.group(1);
    String path = matcher.group(2);
    int status = Integer.parseInt(matcher.group(3));
    String duration = matcher.group(4);

    String statusBlock =
        AnsiOutput.toString(statusBg(status), statusFg(status), String.format(" %3d ", status));
    String durationBlock =
        AnsiOutput.toString(
            AnsiBackground.BRIGHT_WHITE, AnsiColor.BLACK, String.format(" %8s ", duration));
    String methodBlock =
        AnsiOutput.toString(methodBg(method), methodFg(method), centerMethod(method));
    String pathBlock = "\"" + AnsiOutput.toString(AnsiStyle.UNDERLINE, path) + "\"";

    return String.format(" %s | %s | %s %s", statusBlock, durationBlock, methodBlock, pathBlock);
  }

  private String centerMethod(String method) {
    int totalPadding = Math.max(0, METHOD_WIDTH - method.length());
    int left = totalPadding / 2;
    int right = totalPadding - left;
    return " ".repeat(left) + method + " ".repeat(right);
  }

  private AnsiBackground statusBg(int status) {
    if (status >= 500) {
      return AnsiBackground.RED;
    }
    if (status >= 400) {
      return AnsiBackground.BRIGHT_YELLOW;
    }
    if (status >= 300) {
      return AnsiBackground.BRIGHT_CYAN;
    }
    return AnsiBackground.BRIGHT_GREEN;
  }

  private AnsiColor statusFg(int status) {
    if (status >= 500) {
      return AnsiColor.WHITE;
    }
    return AnsiColor.BLACK;
  }

  private AnsiElement methodBg(String method) {
    return switch (method) {
      case "GET" -> Ansi8BitColor.background(18);
      case "POST" -> AnsiBackground.BRIGHT_BLUE;
      case "PUT" -> Ansi8BitColor.background(208);
      case "PATCH" -> AnsiBackground.MAGENTA;
      case "DELETE" -> AnsiBackground.RED;
      default -> AnsiBackground.DEFAULT;
    };
  }

  private AnsiColor methodFg(String method) {
    return switch (method) {
      case "POST" -> AnsiColor.BLACK;
      case "PUT" -> AnsiColor.BLACK;
      default -> AnsiColor.BRIGHT_WHITE;
    };
  }
}
