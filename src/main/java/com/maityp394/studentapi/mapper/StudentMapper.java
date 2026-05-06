package com.maityp394.studentapi.mapper;

import com.maityp394.studentapi.dto.request.CreateStudentRequest;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.entity.Student;
import org.springframework.stereotype.Component;

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
