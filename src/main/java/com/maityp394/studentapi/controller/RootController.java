package com.maityp394.studentapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller handling root endpoint requests for service health and welcome status. */
@RestController
public class RootController {
  /**
   * Returns a welcome message at the root URL.
   *
   * @return a greeting message indicating the API is active
   */
  @GetMapping("/")
  public String root() {
    return "Welcome to the Student API 🚀";
  }
}
