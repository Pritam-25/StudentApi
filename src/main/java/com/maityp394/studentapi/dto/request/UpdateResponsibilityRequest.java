package com.maityp394.studentapi.dto.request;

import com.maityp394.studentapi.entity.Responsibility;
import jakarta.validation.constraints.NotNull;

/**
 * Data transfer object representing the payload for updating a student's institutional
 * responsibility.
 *
 * @param responsibility the new responsibility role to assign
 */
public record UpdateResponsibilityRequest(
    @NotNull(message = "Responsibility is required") Responsibility responsibility) {}
