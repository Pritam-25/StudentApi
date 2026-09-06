package com.maityp394.studentapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Student API Spring Boot application.
 *
 * <p>Bootstraps the application context, configurations, and embedded servlet container.
 */
@SpringBootApplication
public class StudentApiApplication {

  /**
   * Main method used to launch the Spring Boot application.
   *
   * @param args command line arguments passed to the application
   */
  public static void main(String[] args) {
    SpringApplication.run(StudentApiApplication.class, args);
  }
}
