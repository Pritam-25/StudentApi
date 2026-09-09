package com.maityp394.studentapi.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.maityp394.studentapi.entity.Responsibility;
import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object representing the response payload containing student details.
 *
 * @param id the unique identifier of the student
 * @param name the full name of the student
 * @param email the email address of the student
 * @param responsibility the institutional responsibility/role of the student
 * @param createdAt UTC timestamp indicating when the student account was created
 * @param updatedAt UTC timestamp indicating when the student profile was last updated
 */
@JsonPropertyOrder({"id", "name", "email", "responsibility", "createdAt", "updatedAt"})
public record StudentResponse(
    UUID id,
    String name,
    String email,
    Responsibility responsibility,
    Instant createdAt,
    Instant updatedAt) {}
