package com.maityp394.studentapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maityp394.studentapi.entity.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {

}
