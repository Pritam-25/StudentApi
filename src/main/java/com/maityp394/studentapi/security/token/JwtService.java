package com.maityp394.studentapi.security.token;

import com.maityp394.studentapi.config.properties.JwtProperties;
import com.maityp394.studentapi.entity.Student;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Service responsible for minting signed JWT access tokens using Spring Security's JwtEncoder. */
@Service
@RequiredArgsConstructor
public class JwtService {

  private static final long MILLIS_PER_SECOND = 1000L;

  private final JwtEncoder jwtEncoder;
  private final JwtProperties jwtProperties;

  /**
   * Generates a signed JWT access token for the given authenticated student without a session
   * binding.
   *
   * @param student the student entity for whom the token is generated
   * @return the serialized JWT string
   */
  public String generateAccessToken(Student student) {
    return generateAccessToken(student, null);
  }

  /**
   * Generates a signed JWT access token bound to a specific server-side session.
   *
   * @param student the student entity for whom the token is generated
   * @param sessionId the server-side session UUID, or null
   * @return the serialized JWT string
   */
  public String generateAccessToken(Student student, UUID sessionId) {
    Instant now = Instant.now();
    JwtClaimsSet.Builder claimsBuilder =
        JwtClaimsSet.builder()
            .issuer(jwtProperties.issuer())
            .id(UUID.randomUUID().toString())
            .subject(student.getId().toString())
            .claim("email", student.getEmail())
            .claim("authorities", List.of(student.getResponsibility().toAuthority()))
            .issuedAt(now)
            .expiresAt(now.plusMillis(jwtProperties.expirationMs()));

    if (sessionId != null) {
      claimsBuilder.claim("sid", sessionId.toString());
    }

    return jwtEncoder
        .encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claimsBuilder.build()))
        .getTokenValue();
  }

  /**
   * Returns the token expiration duration in seconds.
   *
   * @return expiration time in seconds
   */
  public long getExpirationSeconds() {
    return jwtProperties.expirationMs() / MILLIS_PER_SECOND;
  }
}
