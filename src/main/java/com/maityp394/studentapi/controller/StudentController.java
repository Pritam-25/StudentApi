package com.maityp394.studentapi.controller;

import com.maityp394.studentapi.dto.request.CreateStudentRequest;
import com.maityp394.studentapi.dto.request.PatchStudentRequest;
import com.maityp394.studentapi.dto.request.UpdateStudentRequest;
import com.maityp394.studentapi.dto.response.ApiResponse;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

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

        List<StudentResponse> students = studentService.getAllStudents(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Students fetched successfully", students));
    }

    // GET student by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentById(@PathVariable String id) {
        return ResponseEntity
                .ok(new ApiResponse<>(true, "Student fetched successfully", studentService.getStudentById(id)));
    }

    // PUT - Full Update
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
            @PathVariable String id,
            @Valid @RequestBody UpdateStudentRequest request) {

        StudentResponse updatedStudent = studentService.updateStudent(id, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Student updated successfully", updatedStudent)
        );
    }

    // PATCH - Partial Update
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> patchStudent(
            @PathVariable String id,
            @RequestBody PatchStudentRequest request) {

        StudentResponse updatedStudent = studentService.patchStudent(id, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Student partially updated", updatedStudent)
        );
    }

    // DELETE student by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable String id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Student deleted successfully", null));
    }
}
