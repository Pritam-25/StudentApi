package com.maityp394.studentapi.security;

import com.maityp394.studentapi.entity.Student;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Service responsible for minting signed JWT access tokens using Spring Security's JwtEncoder. */
@Service
public class JwtService {

  private final JwtEncoder jwtEncoder;
  private final long expirationMs;

  /**
   * Constructs a new {@link JwtService}.
   *
   * @param jwtEncoder the configured {@link JwtEncoder} for signing tokens
   * @param expirationMs access token time-to-live in milliseconds
   */
  public JwtService(JwtEncoder jwtEncoder, @Value("${jwt.expiration-ms}") long expirationMs) {
    this.jwtEncoder = jwtEncoder;
    this.expirationMs = expirationMs;
  }

  /**
   * Generates a signed JWT access token for the given authenticated student.
   *
   * @param student the student entity for whom the token is generated
   * @return the serialized JWT string
   */
  public String generateAccessToken(Student student) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(student.getId().toString())
            .claim("email", student.getEmail())
            .claim("authorities", List.of("ROLE_" + student.getResponsibility().name()))
            .issuedAt(now)
            .expiresAt(now.plusMillis(expirationMs))
            .build();

    return jwtEncoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }

  /**
   * Returns the token expiration duration in seconds.
   *
   * @return expiration time in seconds
   */
  public long getExpirationSeconds() {
    return expirationMs / 1000;
  }
}
