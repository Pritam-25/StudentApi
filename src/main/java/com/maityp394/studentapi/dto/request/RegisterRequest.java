package com.maityp394.studentapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object representing the payload for registering a new student.
 *
 * @param name the full name of the student (must be between 2 and 30 characters)
 * @param email the email address of the student (must be a valid email format)
 * @param password the password for the student account (must be between 8 and 30 characters,
 *     containing at least one uppercase letter, at least one number, and at least one special
 *     character)
 */
public record RegisterRequest(
    @NotBlank(message = "Name is required") @Size(min = 2, max = 30, message = "Name must be between 2 and 30 characters") String name,
    @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
    @NotBlank(message = "Password is required") @Size(min = 8, max = 30, message = "Password must be between 8 and 30 characters") @Pattern(
            regexp = "^[^A-Z]*+[A-Z].*+",
            message = "Password must contain at least one uppercase letter")
        @Pattern(regexp = "^\\D*+\\d.*+", message = "Password must contain at least one number") @Pattern(
            regexp = "^[a-zA-Z0-9\\s]*+[^a-zA-Z0-9\\s].*+",
            message = "Password must contain at least one special character")
        String password) {

  public RegisterRequest {
    name = name != null ? name.strip() : null;
    email = email != null ? email.strip().toLowerCase() : null;
  }
}
