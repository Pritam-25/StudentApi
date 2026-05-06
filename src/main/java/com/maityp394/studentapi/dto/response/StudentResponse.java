package com.maityp394.studentapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentResponse {
    private String id;
    private String name;
    private String email;
}
