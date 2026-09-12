package com.maityp394.studentapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/**
 * Data transfer object representing the payload for fully updating a student's profile.
 *
 * @param name the mandatory updated name of the student (must be between 2 and 30 characters)
 * @param email the mandatory updated email address of the student (must be a valid email format)
 */
public record UpdateStudentRequest(
    @NotBlank(message = "Name is required")
        @Size(min = 2, max = 30)
        @Schema(example = "Pritam Maity")
        String name,
    @NotBlank(message = "Email is required") @Email @Schema(example = "pritam.new@example.com")
        String email) {

  public UpdateStudentRequest {
    name = name != null ? name.strip() : null;
    email = email != null ? email.strip().toLowerCase(Locale.ROOT) : null;
  }
}
