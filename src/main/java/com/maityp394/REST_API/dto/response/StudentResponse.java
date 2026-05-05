package com.maityp394.REST_API.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentResponse {
    private String id;
    private String name;
    private String email;
}
