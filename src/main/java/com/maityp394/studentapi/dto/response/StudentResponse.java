package com.maityp394.studentapi.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.maityp394.studentapi.entity.Responsibility;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(example = "43e3966c-69c7-422d-9d86-2623e1476221") UUID id,
    @Schema(example = "Pritam Maity") String name,
    @Schema(example = "pritam@example.com") String email,
    @Schema(example = "STUDENT") Responsibility responsibility,
    @Schema(example = "2026-09-09T10:00:00Z") Instant createdAt,
    @Schema(example = "2026-09-09T10:00:00Z") Instant updatedAt) {}
