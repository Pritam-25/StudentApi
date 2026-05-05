package com.maityp394.REST_API.service;

import com.maityp394.REST_API.dto.request.CreateStudentRequest;
import com.maityp394.REST_API.dto.response.StudentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface StudentService {
    StudentResponse createStudent(CreateStudentRequest request);

    StudentResponse getStudentById(String id);

    Page<StudentResponse> getAllStudents(Pageable pageable);

    void deleteStudent(String id);
}