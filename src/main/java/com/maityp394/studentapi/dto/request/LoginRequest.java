package com.maityp394.studentapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for student authentication (login).
 *
 * @param email the registered student email address
 * @param password the student's password
 */
public record LoginRequest(
    @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
    @NotBlank(message = "Password is required") String password) {}
