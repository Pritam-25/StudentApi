package com.maityp394.studentapi.security.user;

import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom {@link UserDetailsService} bridging persistence data to Spring Security's authentication
 * model.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final StudentRepository studentRepository;

  /**
   * Locates the user based on the email address.
   *
   * @param email the email identifying the student whose data is required
   * @return a fully populated {@link UserDetails} instance
   * @throws UsernameNotFoundException if the student could not be found
   */
  @Override
  @Transactional(readOnly = true)
  public @NonNull UserDetails loadUserByUsername(@NonNull String email)
      throws UsernameNotFoundException {
    Student student =
        studentRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

    return StudentPrincipal.from(student);
  }
}
