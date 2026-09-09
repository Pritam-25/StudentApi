package com.maityp394.studentapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the Student API Spring Boot application.
 *
 * <p>Bootstraps the application context, configurations, and embedded servlet container.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class StudentApiApplication {

  /**
   * Main method used to launch the Spring Boot application.
   *
   * @param args command line arguments passed to the application
   */
  static void main(String[] args) {
    SpringApplication.run(StudentApiApplication.class, args);
  }
}
