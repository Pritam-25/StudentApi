package com.maityp394.studentapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;

/**
 * Payload for student authentication (login).
 *
 * @param email the registered student email address
 * @param password the student's password
 */
public record LoginRequest(
    @NotBlank(message = "Email is required") //
        @Email(message = "Invalid email format") //
        @Schema(example = "pritam@example.com") //
        String email,
    @NotBlank(message = "Password is required") //
        @Schema(example = "Password@123", format = "password") //
        String password) {

  public LoginRequest {
    email = email != null ? email.strip().toLowerCase(Locale.ROOT) : null;
  }
}
