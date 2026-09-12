package com.maityp394.studentapi.security.user;

import com.maityp394.studentapi.entity.Student;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Custom {@link UserDetails} principal that encapsulates the domain {@link Student} entity.
 *
 * <p>Avoids redundant database lookups by providing direct access to the authenticated {@link
 * Student} entity from {@link org.springframework.security.core.Authentication#getPrincipal()}.
 *
 * @param student the underlying authenticated student domain entity
 * @param authorities the granted authorities assigned to the student
 */
public record StudentPrincipal(Student student, Collection<? extends GrantedAuthority> authorities)
    implements UserDetails {

  /**
   * Factory method to build a {@link StudentPrincipal} from a {@link Student} entity.
   *
   * @param student the student entity
   * @return a populated {@link StudentPrincipal}
   */
  public static StudentPrincipal from(Student student) {
    List<GrantedAuthority> authorities =
        List.of(new SimpleGrantedAuthority(student.getResponsibility().toAuthority()));
    return new StudentPrincipal(student, authorities);
  }

  @Override
  public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
    return this.authorities;
  }

  @Override
  public String getPassword() {
    return this.student.getPasswordHash();
  }

  @Override
  public @NonNull String getUsername() {
    return this.student.getEmail();
  }
}
