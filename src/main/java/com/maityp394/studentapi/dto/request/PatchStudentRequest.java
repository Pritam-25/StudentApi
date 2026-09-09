package com.maityp394.studentapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/**
 * Data transfer object representing the payload for partially updating a student. Any non-null
 * fields will be applied to the existing student record.
 *
 * @param name the optional updated name (must be between 2 and 30 characters if provided)
 * @param email the optional updated email address (must be a valid email format if provided)
 */
public record PatchStudentRequest(
    @Size(min = 2, max = 30) //
        @Schema(example = "Pritam Maity") //
        String name,
    @Email //
        @Schema(example = "pritam.patch@example.com") //
        String email) {

  public PatchStudentRequest {
    name = name != null ? name.strip() : null;
    email = email != null ? email.strip().toLowerCase(Locale.ROOT) : null;
  }
}
