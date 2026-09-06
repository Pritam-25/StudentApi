package com.maityp394.studentapi.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logback composite conversion rule dedicated to styling HTTP access log messages emitted by {@code
 * RequestLoggingFilter} in development mode.
 *
 * <p>Wraps messages matching the HTTP pattern with status code background colors, method background
 * colors, duration blocks, and underlined URL paths. Standard application messages pass through
 * unchanged.
 */
public class HttpHighlightConverter extends CompositeConverter<ILoggingEvent> {

  private static final Pattern HTTP_PATTERN =
      Pattern.compile(
          "^(GET|POST|PUT|PATCH|DELETE)\\s+(\\S+)\\s+->\\s+(\\d{3})\\s+\\((\\d+\\s*ms)\\)$");

  private static final String RESET = "\u001B[0m";
  private static final String DURATION_COLOR = "\u001B[47m\u001B[30m"; // White BG, Black FG

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

    return String.format(
        " %s %3d %s | %s %8s %s | %s%s%s \"\u001B[4m%s\u001B[24m\"",
        statusBg(status),
        status,
        RESET,
        DURATION_COLOR,
        duration,
        RESET,
        methodBg(method),
        centerMethod(method),
        RESET,
        path);
  }

  private String centerMethod(String method) {
    return switch (method) {
      case "GET" -> "  GET   ";
      case "PUT" -> "  PUT   ";
      case "POST" -> "  POST  ";
      case "PATCH" -> " PATCH  ";
      case "DELETE" -> " DELETE ";
      default -> {
        int left = Math.max(0, (8 - method.length()) / 2);
        int right = Math.max(0, 8 - method.length() - left);
        yield " ".repeat(left) + method + " ".repeat(right);
      }
    };
  }

  private String statusBg(int status) {
    if (status >= 500) return "\u001B[41m\u001B[37m"; // Red BG, White FG
    if (status >= 400) return "\u001B[43m\u001B[30m"; // Yellow BG, Black FG
    if (status >= 300) return "\u001B[46m\u001B[37m"; // Cyan BG, White FG
    return "\u001B[42m\u001B[30m"; // Green BG, Black FG
  }

  private String methodBg(String method) {
    return switch (method) {
      case "GET" -> "\u001B[48;5;18m\u001B[37m"; // Navy BG, White FG
      case "POST" -> "\u001B[44m\u001B[37m"; // Blue BG, White FG
      case "PUT" -> "\u001B[48;5;214m\u001B[30m"; // Orange BG, Black FG
      case "PATCH" -> "\u001B[45m\u001B[37m"; // Magenta BG, White FG
      case "DELETE" -> "\u001B[41m\u001B[37m"; // Red BG, White FG
      default -> RESET;
    };
  }
}
