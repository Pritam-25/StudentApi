package com.maityp394.studentapi.security.oauth2;

import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Custom OIDC user service that intercepts OpenID Connect user details returned by Google,
 * validates email verification status, and delegates account provisioning/linking to {@link
 * AuthService}.
 */
@Service
@Slf4j
public class CustomOidcUserService extends OidcUserService {

  private final AuthService authService;

  public CustomOidcUserService(@Lazy AuthService authService) {
    this.authService = authService;
  }

  @Override
  public @NonNull OidcUser loadUser(@NonNull OidcUserRequest userRequest)
      throws OAuth2AuthenticationException {
    OidcUser oidcUser = super.loadUser(userRequest);

    String email = oidcUser.getEmail();
    if (email == null || email.isBlank()) {
      log.warn("Google OIDC token did not contain an email claim");
      throw new OAuth2AuthenticationException(
          new OAuth2Error("missing_email", "Email claim not found in Google user profile", null),
          "Email claim not found in Google user profile");
    }

    Boolean emailVerified = oidcUser.getEmailVerified();
    if (emailVerified == null || !emailVerified) {
      log.warn("Rejected Google OAuth login for unverified email: {}", email);
      throw new OAuth2AuthenticationException(
          new OAuth2Error("unverified_email", "Email address is not verified by Google", null),
          "Email address is not verified by Google");
    }

    String sub = oidcUser.getSubject();
    String name =
        (oidcUser.getFullName() != null && !oidcUser.getFullName().isBlank())
            ? oidcUser.getFullName()
            : oidcUser.getGivenName();

    Student student = authService.processGoogleUser(sub, email, name);
    return new StudentOidcUser(student, oidcUser);
  }
}
