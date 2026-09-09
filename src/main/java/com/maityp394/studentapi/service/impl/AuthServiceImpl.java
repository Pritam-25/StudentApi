package com.maityp394.studentapi.service.impl;

import com.maityp394.studentapi.dto.request.LoginRequest;
import com.maityp394.studentapi.dto.request.RegisterRequest;
import com.maityp394.studentapi.dto.response.AuthResult;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.entity.Student;
import com.maityp394.studentapi.exception.DuplicateResourceException;
import com.maityp394.studentapi.exception.ErrorCode;
import com.maityp394.studentapi.exception.ResourceNotFoundException;
import com.maityp394.studentapi.mapper.StudentMapper;
import com.maityp394.studentapi.repository.StudentRepository;
import com.maityp394.studentapi.security.JwtService;
import com.maityp394.studentapi.security.StudentPrincipal;
import com.maityp394.studentapi.service.AuthService;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AuthService} managing authentication workflows including student
 * registration, login with JWT issuance, and current authenticated user retrieval.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthServiceImpl implements AuthService {

  private final StudentRepository studentRepository;
  private final AuthenticationManager authenticationManager;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final StudentMapper studentMapper;

  /** {@inheritDoc} */
  @Override
  public AuthResult register(RegisterRequest request) {
    if (studentRepository.existsByEmail(request.email())) {
      throw new DuplicateResourceException(ErrorCode.STUDENT_EMAIL_ALREADY_EXISTS);
    }

    String passwordHash = passwordEncoder.encode(request.password());
    Student student = studentMapper.toEntity(request, passwordHash);
    Student saved = studentRepository.saveAndFlush(student);

    log.info("Student registered successfully: id={}", saved.getId());
    String token = jwtService.generateAccessToken(saved);
    StudentResponse studentResponse = studentMapper.toResponse(saved);
    return new AuthResult(studentResponse, token, jwtService.getExpirationSeconds());
  }

  /** {@inheritDoc} */
  @Override
  public AuthResult login(LoginRequest request) {
    Authentication authenticationRequest =
        UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password());

    Authentication authResult = authenticationManager.authenticate(authenticationRequest);
    StudentPrincipal principal = (StudentPrincipal) authResult.getPrincipal();
    Student student = Objects.requireNonNull(principal).student();

    String token = jwtService.generateAccessToken(student);
    StudentResponse studentResponse = studentMapper.toResponse(student);

    log.info("Student Logged in successfully as {}", studentResponse.responsibility());
    return new AuthResult(studentResponse, token, jwtService.getExpirationSeconds());
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public StudentResponse getCurrentUser(UUID studentId) {
    Student student =
        studentRepository
            .findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STUDENT_NOT_FOUND));

    log.info("Current Student fetched successfully.");
    return studentMapper.toResponse(student);
  }
}
