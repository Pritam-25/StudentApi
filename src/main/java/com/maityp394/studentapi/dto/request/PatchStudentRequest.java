package com.maityp394.studentapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record PatchStudentRequest(

        @Size(min = 2, max = 30)
        String name,

        @Email
        String email
) {
}
