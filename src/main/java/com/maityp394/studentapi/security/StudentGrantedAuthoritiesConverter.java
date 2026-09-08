package com.maityp394.studentapi.security;

import com.maityp394.studentapi.repository.StudentRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Dynamically resolves a student's granted authorities from the database for each JWT.
 *
 * <p>Decouples {@link SecurityConfig} from persistence repositories while ensuring role demotions
 * and updates take effect immediately on subsequent requests.
 */
@Component
@RequiredArgsConstructor
public class StudentGrantedAuthoritiesConverter
    implements Converter<Jwt, Collection<GrantedAuthority>> {

  private final StudentRepository studentRepository;

  @Override
  public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
    String subject = jwt.getSubject();
    if (subject == null || subject.isBlank()) {
      return List.of();
    }
    try {
      UUID studentId = UUID.fromString(subject);
      return studentRepository
          .findById(studentId)
          .map(
              student ->
                  List.<GrantedAuthority>of(
                      new SimpleGrantedAuthority(student.getResponsibility().toAuthority())))
          .orElseGet(List::of);
    } catch (IllegalArgumentException _) {
      return List.of();
    }
  }
}
