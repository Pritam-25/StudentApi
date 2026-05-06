package com.maityp394.studentapi.service;

import com.maityp394.studentapi.dto.request.CreateStudentRequest;
import com.maityp394.studentapi.dto.request.PatchStudentRequest;
import com.maityp394.studentapi.dto.request.UpdateStudentRequest;
import com.maityp394.studentapi.dto.response.StudentResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StudentService {
    StudentResponse createStudent(CreateStudentRequest request);

    StudentResponse getStudentById(String id);

    List<StudentResponse> getAllStudents(Pageable pageable);

    StudentResponse updateStudent(String id, UpdateStudentRequest request);

    StudentResponse patchStudent(String id, PatchStudentRequest request);

    void deleteStudent(String id);
}