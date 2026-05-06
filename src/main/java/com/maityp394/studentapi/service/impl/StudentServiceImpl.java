package com.maityp394.studentapi.service.impl;

import com.maityp394.studentapi.dto.request.CreateStudentRequest;
import com.maityp394.studentapi.dto.request.PatchStudentRequest;
import com.maityp394.studentapi.dto.request.UpdateStudentRequest;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.exception.ResourceNotFoundException;
import com.maityp394.studentapi.mapper.StudentMapper;
import com.maityp394.studentapi.repository.StudentRepository;
import com.maityp394.studentapi.service.StudentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public List<StudentResponse> getAllStudents(Pageable pageable) {
        Page<Student> page = studentRepository.findAll(
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.DESC, "name"))));

        return page.map(studentMapper::tResponse).getContent();
    }

    @Override
    public StudentResponse updateStudent(String id, UpdateStudentRequest request) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        //  FULL REPLACE
        student.setName(request.name());
        student.setEmail(request.email());

        Student updatedStudent = studentRepository.save(student);
        return studentMapper.tResponse(updatedStudent);
    }

    @Override
    public StudentResponse patchStudent(String id, PatchStudentRequest request) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        // PARTIAL UPDATE (only non-null fields)
        if (request.name() != null) {
            student.setName(request.name());
        }

        if (request.email() != null) {
            student.setEmail(request.email());
        }

        Student updatedStudent = studentRepository.save(student);
        return studentMapper.tResponse(updatedStudent);
    }


    @Override
    public void deleteStudent(String id) {
        studentRepository.deleteById(id);
    }

}
