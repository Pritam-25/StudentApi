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
import com.maityp394.studentapi.security.session.RedisSessionService;
import com.maityp394.studentapi.security.token.AuthTokens;
import com.maityp394.studentapi.security.token.RefreshToken;
import com.maityp394.studentapi.security.token.TokenService;
import com.maityp394.studentapi.security.user.StudentPrincipal;
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
 * registration, login with unified token and Redis session issuance, and refresh token rotation.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthServiceImpl implements AuthService {

  private final StudentRepository studentRepository;
  private final AuthenticationManager authenticationManager;
  private final PasswordEncoder passwordEncoder;
  private final StudentMapper studentMapper;
  private final TokenService tokenService;
  private final RedisSessionService redisSessionService;

  /** {@inheritDoc} */
  @Override
  @Transactional
  public AuthResult register(RegisterRequest request, String userAgent, String ipAddress) {
    if (studentRepository.existsByEmail(request.email())) {
      throw new DuplicateResourceException(ErrorCode.STUDENT_EMAIL_ALREADY_EXISTS);
    }

    String passwordHash = passwordEncoder.encode(request.password());
    Student student = studentMapper.toEntity(request, passwordHash);
    Student saved = studentRepository.saveAndFlush(student);

    log.info("Student registered successfully: id={}", saved.getId());
    AuthTokens tokens = tokenService.issueTokens(saved, userAgent, ipAddress);
    StudentResponse studentResponse = studentMapper.toResponse(saved);
    return new AuthResult(studentResponse, tokens);
  }

  /** {@inheritDoc} */
  @Override
  public AuthResult login(LoginRequest request, String userAgent, String ipAddress) {
    Authentication authenticationRequest =
        UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password());

    Authentication authResult = authenticationManager.authenticate(authenticationRequest);
    StudentPrincipal principal = (StudentPrincipal) authResult.getPrincipal();
    Student student = Objects.requireNonNull(principal).student();

    AuthTokens tokens = tokenService.issueTokens(student, userAgent, ipAddress);
    StudentResponse studentResponse = studentMapper.toResponse(student);

    log.info("Student logged in successfully as {}", studentResponse.responsibility());
    return new AuthResult(studentResponse, tokens);
  }

  /** {@inheritDoc} */
  @Override
  public AuthTokens refresh(String rawRefreshToken, String userAgent, String ipAddress) {
    return tokenService.rotateTokens(rawRefreshToken, userAgent, ipAddress);
  }

  /** {@inheritDoc} */
  @Override
  public void logout(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      return;
    }
    try {
      RefreshToken token = RefreshToken.parse(rawRefreshToken);
      redisSessionService.revokeSession(token.sessionId());
      log.info("Logged out session {}", token.sessionId());
    } catch (Exception e) {
      log.warn("Failed to parse refresh token during logout: {}", e.getMessage());
    }
  }

  /** {@inheritDoc} */
  @Override
  public void logoutAll(UUID userId) {
    redisSessionService.revokeAllUserSessions(userId);
    log.info("Logged out all sessions for user {}", userId);
  }

  /** {@inheritDoc} */
  @Override
  public StudentResponse getCurrentUser(UUID studentId) {
    Student student =
        studentRepository
            .findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STUDENT_NOT_FOUND));

    log.info("Current student fetched successfully: id={}", studentId);
    return studentMapper.toResponse(student);
  }
}
