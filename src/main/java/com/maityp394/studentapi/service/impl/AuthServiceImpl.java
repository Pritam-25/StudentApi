package com.maityp394.studentapi.service.impl;

import com.maityp394.studentapi.dto.request.LoginRequest;
import com.maityp394.studentapi.dto.request.RegisterRequest;
import com.maityp394.studentapi.dto.response.AuthResponse;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.exception.DuplicateResourceException;
import com.maityp394.studentapi.exception.ErrorCode;
import com.maityp394.studentapi.exception.ResourceNotFoundException;
import com.maityp394.studentapi.mapper.StudentMapper;
import com.maityp394.studentapi.repository.StudentRepository;
import com.maityp394.studentapi.security.JwtService;
import com.maityp394.studentapi.service.AuthService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AuthService} managing authentication workflows including student
 * registration, login with JWT issuance, and current authenticated user retrieval.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

  private final StudentRepository studentRepository;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final StudentMapper studentMapper;

  /** {@inheritDoc} */
  @Override
  public StudentResponse register(RegisterRequest request) {
    String email = request.email().trim().toLowerCase();

    if (studentRepository.existsByEmail(email)) {
      throw new DuplicateResourceException(ErrorCode.STUDENT_EMAIL_ALREADY_EXISTS);
    }

    Student student = studentMapper.toEntity(request);
    Student saved = studentRepository.save(student);

    return studentMapper.toResponse(saved);
  }

  /** {@inheritDoc} */
  @Override
  public AuthResponse login(LoginRequest request) {
    String email = request.email().trim().toLowerCase();

    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(email, request.password()));

    Student student =
        studentRepository
            .findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STUDENT_NOT_FOUND));

    String token = jwtService.generateAccessToken(student);

    return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds());
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public StudentResponse getCurrentUser(UUID studentId) {
    Student student =
        studentRepository
            .findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STUDENT_NOT_FOUND));
    return studentMapper.toResponse(student);
  }
}
