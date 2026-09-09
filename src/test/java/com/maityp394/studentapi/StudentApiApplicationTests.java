package com.maityp394.studentapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Smoke test verifying that the complete Spring Boot application context starts up cleanly. */
@SpringBootTest
@DisplayName("Application Context Startup Smoke Test")
class StudentApiApplicationTests {

  @Test
  @DisplayName("Context loads successfully without exceptions")
  void contextLoads() {}
}
