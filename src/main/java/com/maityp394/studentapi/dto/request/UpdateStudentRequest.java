package com.maityp394.studentapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStudentRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 30)
        String name,

        @NotBlank(message = "Email is required")
        @Email
        String email
) {
}


