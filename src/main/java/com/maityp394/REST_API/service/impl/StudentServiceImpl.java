package com.maityp394.REST_API.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.maityp394.REST_API.dto.request.CreateStudentRequest;
import com.maityp394.REST_API.dto.response.StudentResponse;
import com.maityp394.REST_API.entity.Student;
import com.maityp394.REST_API.exception.ResourceNotFoundException;
import com.maityp394.REST_API.mapper.StudentMapper;
import com.maityp394.REST_API.repository.StudentRepository;
import com.maityp394.REST_API.service.StudentService;

@Service
// @RequiredArgsConstructor -> generate constructor by default
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    public StudentServiceImpl(StudentRepository studentRepository, StudentMapper studentMapper) {
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
    }

    @Override
    public StudentResponse createStudent(CreateStudentRequest request) {
        Student student = studentMapper.toEntity(request);
        Student newStudent = studentRepository.save(student);
        return studentMapper.tResponse(newStudent);
    }

    @Override
    public StudentResponse getStudentById(String id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        return studentMapper.tResponse(student);
    }

    @Override
    public Page<StudentResponse> getAllStudents(Pageable pagable) {
        return studentRepository.findAll(pagable)
                .map(studentMapper::tResponse);
    }

    @Override
    public void deleteStudent(String id) {
        studentRepository.deleteById(id);
    }

}
