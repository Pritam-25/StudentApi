package com.maityp394.studentapi.security.oauth2;

import com.maityp394.studentapi.entity.Student;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Custom {@link OidcUser} implementation wrapping an authenticated {@link Student} domain entity.
 *
 * <p>Delegates standard OpenID Connect claim extraction to the underlying {@link OidcUser} while
 * binding authorities directly to the student's assigned {@link
 * com.maityp394.studentapi.entity.Responsibility}.
 */
@Getter
public class StudentOidcUser implements OidcUser {

  private final Student student;
  private final OidcUser delegate;
  private final Collection<? extends GrantedAuthority> authorities;

  /**
   * Constructs a new {@link StudentOidcUser}.
   *
   * @param student the domain student entity
   * @param delegate the standard Spring Security OIDC user
   */
  public StudentOidcUser(Student student, OidcUser delegate) {
    this.student = student;
    this.delegate = delegate;
    this.authorities =
        List.of(new SimpleGrantedAuthority(student.getResponsibility().toAuthority()));
  }

  @Override
  public @NonNull Map<String, Object> getClaims() {
    return delegate.getClaims();
  }

  @Override
  public OidcUserInfo getUserInfo() {
    return delegate.getUserInfo();
  }

  @Override
  public @NonNull OidcIdToken getIdToken() {
    return delegate.getIdToken();
  }

  @Override
  public @NonNull Map<String, Object> getAttributes() {
    return delegate.getAttributes();
  }

  @Override
  public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
    return this.authorities;
  }

  @Override
  public @NonNull String getName() {
    return this.student.getEmail();
  }
}
