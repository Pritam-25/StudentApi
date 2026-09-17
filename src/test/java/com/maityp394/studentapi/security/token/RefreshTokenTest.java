package com.maityp394.studentapi.security.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RefreshToken Unit Tests")
class RefreshTokenTest {

  @Test
  @DisplayName("create should produce valid structured token with expected prefix and session ID")
  void shouldCreateValidRefreshToken() {
    UUID sessionId = UUID.randomUUID();
    RefreshToken token = RefreshToken.create(sessionId);

    assertThat(token.sessionId()).isEqualTo(sessionId);
    assertThat(token.secret()).isNotBlank();
    assertThat(token.toTokenString()).startsWith("rt_" + sessionId + "_");
    assertThat(token.hash()).isEqualTo(RefreshToken.hash(token.secret()));
  }

  @Test
  @DisplayName("parse should reconstruct RefreshToken correctly from formatted string")
  void shouldParseValidFormattedToken() {
    UUID sessionId = UUID.randomUUID();
    RefreshToken original = RefreshToken.create(sessionId);
    String formatted = original.toTokenString();

    RefreshToken parsed = RefreshToken.parse(formatted);

    assertThat(parsed.sessionId()).isEqualTo(original.sessionId());
    assertThat(parsed.secret()).isEqualTo(original.secret());
  }

  @Test
  @DisplayName("parse should throw IllegalArgumentException for malformed strings")
  void shouldRejectMalformedTokenStrings() {
    assertThatThrownBy(() -> RefreshToken.parse(null)).isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> RefreshToken.parse("invalid_token"))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> RefreshToken.parse("rt_not-a-uuid_secret"))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> RefreshToken.parse("rt_uuid-without-separator"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("hash and matches should verify secrets correctly using SHA-256")
  void shouldHashAndMatchSecretsAccurately() {
    String secret = "super-secret-random-token-value-12345";
    String hash = RefreshToken.hash(secret);

    assertThat(hash).isNotNull().hasSize(64); // 256 bits = 64 hex characters
    assertThat(RefreshToken.matches(secret, hash)).isTrue();
    assertThat(RefreshToken.matches("wrong-secret", hash)).isFalse();
    assertThat(RefreshToken.matches(null, hash)).isFalse();
    assertThat(RefreshToken.matches(secret, null)).isFalse();
  }
}
