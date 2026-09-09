package com.maityp394.studentapi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.maityp394.studentapi.config.properties.JwtProperties;
import com.maityp394.studentapi.entity.Responsibility;
import com.maityp394.studentapi.entity.Student;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

  @ParameterizedTest(name = "expirationMs={0}ms should convert to {1}s")
  @CsvSource({"1000, 1", "1500, 1", "2000, 2", "60000, 60", "900000, 900"})
  @DisplayName("getExpirationSeconds should convert expiration milliseconds to seconds")
  void shouldConvertExpirationSecondsCorrectly(long expirationMs, long expectedSeconds) {
    JwtProperties properties = new JwtProperties("secret", "issuer", expirationMs);
    JwtEncoder encoder = mock(JwtEncoder.class);
    JwtService jwtService = new JwtService(encoder, properties);

    assertThat(jwtService.getExpirationSeconds()).isEqualTo(expectedSeconds);
  }

  @Test
  @DisplayName("generateAccessToken should build claims and return encoded token")
  void shouldGenerateAccessToken() {
    JwtProperties properties = new JwtProperties("secret", "https://api.test", 900000L);
    JwtEncoder encoder = mock(JwtEncoder.class);
    Jwt mockJwt = mock(Jwt.class);
    when(mockJwt.getTokenValue()).thenReturn("mocked.jwt.token");
    when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

    JwtService jwtService = new JwtService(encoder, properties);

    Student student = new Student();
    student.setId(UUID.randomUUID());
    student.setName("Test Student");
    student.setEmail("test@example.com");
    student.setResponsibility(Responsibility.STUDENT);

    String token = jwtService.generateAccessToken(student);

    assertThat(token).isEqualTo("mocked.jwt.token");
  }
}
