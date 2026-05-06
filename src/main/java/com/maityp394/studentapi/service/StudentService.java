package com.maityp394.studentapi.service;

import org.springframework.data.domain.Pageable;
import java.util.List;
import com.maityp394.studentapi.dto.request.CreateStudentRequest;
import com.maityp394.studentapi.dto.response.StudentResponse;

public interface StudentService {
    StudentResponse createStudent(CreateStudentRequest request);

    StudentResponse getStudentById(String id);

    List<StudentResponse> getAllStudents(Pageable pageable);

    void deleteStudent(String id);
}