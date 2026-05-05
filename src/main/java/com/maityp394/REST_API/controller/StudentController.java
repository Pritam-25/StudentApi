package com.maityp394.REST_API.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.maityp394.REST_API.dto.request.CreateStudentRequest;
import com.maityp394.REST_API.dto.response.ApiResponse;
import com.maityp394.REST_API.dto.response.StudentResponse;
import com.maityp394.REST_API.service.StudentService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URI;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // POST create Student
    @PostMapping
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
            @Valid @RequestBody CreateStudentRequest request,
            UriComponentsBuilder ucbBuilder) {
        StudentResponse createdStudent = studentService.createStudent(request);
        URI location = ucbBuilder.path("/api/v1/students/{id}").buildAndExpand(createdStudent.getId()).toUri();
        return ResponseEntity.created(location)
                .body(new ApiResponse<>(true, "Student created successfully", createdStudent));
    }

    // GET all Students
    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getStudents(
            @PageableDefault(size = 5, sort = "name") Pageable pageable) {

        Page<StudentResponse> studentPage = studentService.getAllStudents(pageable);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Students fetched successfully", studentPage.getContent()));
    }

    // GET student by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentById(@PathVariable String id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Student fetched successfully", studentService.getStudentById(id)));
    }

    // DELETE student by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable String id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Student deleted successfully", null));
    }
}
