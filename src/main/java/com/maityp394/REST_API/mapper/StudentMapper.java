package com.maityp394.REST_API.mapper;

import org.springframework.stereotype.Component;
import com.maityp394.REST_API.dto.request.CreateStudentRequest;
import com.maityp394.REST_API.dto.response.StudentResponse;
import com.maityp394.REST_API.entity.Student;

@Component
public class StudentMapper {
    public Student toEntity(CreateStudentRequest req) {
        Student student = new Student();
        student.setName(req.name());
        student.setEmail(req.email());
        student.setPassword(req.password());
        return student;
    }

    public StudentResponse tResponse(Student student) {
        return new StudentResponse(
            student.getId(),
            student.getName(),
            student.getEmail()
        );
    }
}
