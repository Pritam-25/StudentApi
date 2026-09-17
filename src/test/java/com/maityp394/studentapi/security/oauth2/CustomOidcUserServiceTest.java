package com.maityp394.studentapi.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.maityp394.studentapi.entity.Responsibility;
import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.service.AuthService;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@DisplayName("CustomOidcUserService Unit Tests")
class CustomOidcUserServiceTest {

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService = mock(AuthService.class);
  }

  private CustomOidcUserService createServiceWithDelegate(OidcUser delegateUser) {
    return new CustomOidcUserService(authService) {
      @Override
      public @NonNull OidcUser loadUser(@NonNull OidcUserRequest userRequest)
          throws OAuth2AuthenticationException {
        String email = delegateUser.getEmail();
        if (email == null || email.isBlank()) {
          throw new OAuth2AuthenticationException(
              new org.springframework.security.oauth2.core.OAuth2Error("missing_email"),
              "missing_email");
        }
        Boolean emailVerified = delegateUser.getEmailVerified();
        if (emailVerified == null || !emailVerified) {
          throw new OAuth2AuthenticationException(
              new org.springframework.security.oauth2.core.OAuth2Error("unverified_email"),
              "unverified_email");
        }
        String name =
            (delegateUser.getFullName() != null && !delegateUser.getFullName().isBlank())
                ? delegateUser.getFullName()
                : delegateUser.getGivenName();
        Student student = authService.processGoogleUser(delegateUser.getSubject(), email, name);
        return new StudentOidcUser(student, delegateUser);
      }
    };
  }

  @Test
  @DisplayName("loadUser should successfully process verified Google user")
  void shouldProcessVerifiedGoogleUser() {
    Map<String, Object> claims =
        Map.of(
            "sub", "google-12345",
            "email", "pritam@example.com",
            "email_verified", true,
            "name", "Pritam Maity");

    OidcIdToken idToken =
        new OidcIdToken("token-val", Instant.now(), Instant.now().plusSeconds(3600), claims);
    OidcUserInfo userInfo = new OidcUserInfo(claims);
    OidcUser delegate = new DefaultOidcUser(Set.of(), idToken, userInfo);

    Student student =
        new Student("Pritam Maity", "pritam@example.com", Responsibility.STUDENT, "google-12345");
    when(authService.processGoogleUser("google-12345", "pritam@example.com", "Pritam Maity"))
        .thenReturn(student);

    CustomOidcUserService service = createServiceWithDelegate(delegate);
    OidcUser result = service.loadUser(mock(OidcUserRequest.class));

    assertThat(result).isInstanceOf(StudentOidcUser.class);
    StudentOidcUser studentOidcUser = (StudentOidcUser) result;
    assertThat(studentOidcUser.getStudent()).isEqualTo(student);
    assertThat(studentOidcUser.getAuthorities()).extracting("authority").contains("ROLE_STUDENT");
  }

  @Test
  @DisplayName("loadUser should fallback to given_name when full name is missing")
  void shouldFallbackToGivenNameWhenFullNameIsMissing() {
    Map<String, Object> claims =
        Map.of(
            "sub", "google-54321",
            "email", "pritam@example.com",
            "email_verified", true,
            "given_name", "Pritam");

    OidcIdToken idToken =
        new OidcIdToken("token-val", Instant.now(), Instant.now().plusSeconds(3600), claims);
    OidcUserInfo userInfo = new OidcUserInfo(claims);
    OidcUser delegate = new DefaultOidcUser(Set.of(), idToken, userInfo);

    Student student =
        new Student("Pritam", "pritam@example.com", Responsibility.STUDENT, "google-54321");
    when(authService.processGoogleUser("google-54321", "pritam@example.com", "Pritam"))
        .thenReturn(student);

    CustomOidcUserService service = createServiceWithDelegate(delegate);
    OidcUser result = service.loadUser(mock(OidcUserRequest.class));

    assertThat(result).isInstanceOf(StudentOidcUser.class);
    StudentOidcUser studentOidcUser = (StudentOidcUser) result;
    assertThat(studentOidcUser.getStudent()).isEqualTo(student);
  }

  @Test
  @DisplayName("loadUser should reject unverified email")
  void shouldRejectUnverifiedEmail() {
    Map<String, Object> claims =
        Map.of(
            "sub", "google-12345",
            "email", "unverified@example.com",
            "email_verified", false,
            "name", "Unverified User");

    OidcIdToken idToken =
        new OidcIdToken("token-val", Instant.now(), Instant.now().plusSeconds(3600), claims);
    OidcUserInfo userInfo = new OidcUserInfo(claims);
    OidcUser delegate = new DefaultOidcUser(Set.of(), idToken, userInfo);

    CustomOidcUserService service = createServiceWithDelegate(delegate);

    assertThatThrownBy(() -> service.loadUser(mock(OidcUserRequest.class)))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .hasMessageContaining("unverified_email");
  }

  @Test
  @DisplayName("loadUser should reject user without email claim")
  void shouldRejectMissingEmail() {
    Map<String, Object> claims =
        Map.of(
            "sub", "google-12345",
            "name", "No Email User");

    OidcIdToken idToken =
        new OidcIdToken("token-val", Instant.now(), Instant.now().plusSeconds(3600), claims);
    OidcUserInfo userInfo = new OidcUserInfo(claims);
    OidcUser delegate = new DefaultOidcUser(Set.of(), idToken, userInfo);

    CustomOidcUserService service = createServiceWithDelegate(delegate);

    assertThatThrownBy(() -> service.loadUser(mock(OidcUserRequest.class)))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .hasMessageContaining("missing_email");
  }
}
