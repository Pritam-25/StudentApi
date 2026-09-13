/**
 * Exception handling and RFC 9457 Problem Details infrastructure for the Student API.
 *
 * <p>This package centralizes application-wide exception translation, converting business domain
 * exceptions, Spring MVC validation failures, serialization errors, database integrity conflicts,
 * and security rejections into uniform, machine-readable HTTP problem responses conforming to RFC
 * 9457.
 *
 * <h2>1. Core Package Components</h2>
 *
 * <ul>
 *   <li>{@link com.maityp394.studentapi.exception.GlobalExceptionHandler} - Central
 *       {@code @RestControllerAdvice} controller interceptor managing standard HTTP error
 *       translation.
 *   <li>{@link com.maityp394.studentapi.exception.ProblemDetailFactory} - Shared factory for
 *       creating consistent {@link org.springframework.http.ProblemDetail} instances enriched with
 *       standard application extension properties.
 *   <li>{@link com.maityp394.studentapi.exception.ErrorCode} - Canonical catalog of business,
 *       validation, security, and infrastructure error identifiers, titles, and HTTP status codes.
 *   <li>{@link com.maityp394.studentapi.exception.ApplicationException} - Abstract base class for
 *       domain runtime exceptions carrying an associated {@link
 *       com.maityp394.studentapi.exception.ErrorCode}.
 *   <li>{@link com.maityp394.studentapi.exception.ResourceNotFoundException} - Thrown when a
 *       requested entity is not found (HTTP 404).
 *   <li>{@link com.maityp394.studentapi.exception.DuplicateResourceException} - Thrown on business
 *       uniqueness conflicts, such as duplicate email registrations (HTTP 409).
 *   <li>{@link com.maityp394.studentapi.exception.ForbiddenException} - Thrown when an
 *       authenticated principal is denied access to a specific resource (HTTP 403).
 *   <li>{@link com.maityp394.studentapi.exception.InvalidTokenException} - Thrown on malformed,
 *       expired, or revoked session/refresh tokens (HTTP 401).
 * </ul>
 *
 * <h2>2. GlobalExceptionHandler Mapping Reference</h2>
 *
 * <p>The table below summarizes each handler method defined in {@link
 * com.maityp394.studentapi.exception.GlobalExceptionHandler}, the source exception it intercepts,
 * the resulting HTTP status, and the emitted machine-readable error code:
 *
 * <table border="1">
 *   <caption>Exception Handler Dispatch Mapping</caption>
 *   <tr>
 *     <th>Handler Method</th>
 *     <th>Intercepted Exception</th>
 *     <th>HTTP Status</th>
 *     <th>Application Error Code</th>
 *     <th>Description &amp; Trigger Scenario</th>
 *   </tr>
 *   <tr>
 *     <td>{@code handleApplicationException}</td>
 *     <td>{@link com.maityp394.studentapi.exception.ApplicationException}</td>
 *     <td>Dynamic (400, 401, 403, 404, 409)</td>
 *     <td>{@code ex.getErrorCode()}</td>
 *     <td>Domain business exceptions (e.g. {@code ResourceNotFoundException}, {@code DuplicateResourceException}).</td>
 *   </tr>
 *   <tr>
 *     <td>{@code handleMethodArgumentNotValid}</td>
 *     <td>{@link org.springframework.web.bind.MethodArgumentNotValidException}</td>
 *     <td>400 Bad Request</td>
 *     <td>{@code VALIDATION_FAILED}</td>
 *     <td>JSON request body DTO validation failures (triggered by {@code @Valid @RequestBody}) with field-level errors map.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code handleHandlerMethodValidationException}</td>
 *     <td>{@link org.springframework.web.method.annotation.HandlerMethodValidationException}</td>
 *     <td>400 Bad Request</td>
 *     <td>{@code VALIDATION_FAILED}</td>
 *     <td>Method parameter validation failures (e.g. constraints placed on {@code @RequestParam} or {@code @PathVariable}).</td>
 *   </tr>
 *   <tr>
 *     <td>{@code handleMethodArgumentTypeMismatch}</td>
 *     <td>{@link org.springframework.web.method.annotation.MethodArgumentTypeMismatchException}</td>
 *     <td>400 Bad Request</td>
 *     <td>{@code INVALID_PARAMETER_TYPE}</td>
 *     <td>Parameter type conversion failures (e.g. passing a non-UUID string into a {@code UUID} path variable).</td>
 *   </tr>
 *   <tr>
 *     <td>{@code handlePropertyReference}</td>
 *     <td>{@link org.springframework.data.core.PropertyReferenceException}</td>
 *     <td>400 Bad Request</td>
 *     <td>{@code INVALID_REQUEST}</td>
 *     <td>Invalid sort property names in pagination queries (e.g. {@code ?sort=name,invalidField} or unknown entity properties).</td>
 *   </tr>
 *   <tr>
 *     <td>{@code handleDataIntegrityViolation}</td>
 *     <td>{@link org.springframework.dao.DataIntegrityViolationException}</td>
 *     <td>409 Conflict</td>
 *     <td>{@code DATA_INTEGRITY_VIOLATION}</td>
 *     <td>Persistence layer database constraint violations (e.g. unique or foreign key constraint collisions).</td>
 *   </tr>
 *   <tr>
 *     <td>{@code handleBadCredentials}</td>
 *     <td>{@link org.springframework.security.authentication.BadCredentialsException}</td>
 *     <td>401 Unauthorized</td>
 *     <td>{@code INVALID_CREDENTIALS}</td>
 *     <td>Authentication failures resulting from invalid email or password credentials.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code handleAccessDenied}</td>
 *     <td>{@link org.springframework.security.access.AccessDeniedException}</td>
 *     <td>403 Forbidden</td>
 *     <td>{@code FORBIDDEN}</td>
 *     <td>Spring Security authorization failures (e.g. {@code @IsClassRepresentative} permission rejections).</td>
 *   </tr>
 *   <tr>
 *     <td>{@code handleUnexpectedException}</td>
 *     <td>{@link java.lang.Exception}</td>
 *     <td>500 Internal Server Error</td>
 *     <td>{@code INTERNAL_SERVER_ERROR}</td>
 *     <td>Fallback safety net for unhandled runtime/system exceptions; logs diagnostic stack traces internally while sanitizing client response.</td>
 *   </tr>
 * </table>
 *
 * <h2>3. RFC 9457 Response Payload Structure</h2>
 *
 * <p>All error responses generated by this package include standard RFC 9457 properties alongside
 * custom extensions:
 *
 * <ul>
 *   <li>{@code type} - Standard problem type URI (defaults to {@code "about:blank"}).
 *   <li>{@code title} - Short, human-readable summary of the problem type.
 *   <li>{@code status} - HTTP status code matching the response header.
 *   <li>{@code detail} - Human-readable explanation specific to this error occurrence.
 *   <li>{@code instance} - Request URI identifying the specific occurrence of the problem.
 *   <li>{@code code} - Machine-readable {@link com.maityp394.studentapi.exception.ErrorCode} string
 *       (e.g. {@code "STUDENT_NOT_FOUND"}).
 *   <li>{@code timestamp} - UTC ISO-8601 instant indicating when the error was captured.
 *   <li>{@code errors} - Key-value map of field/parameter validation failures (present only on
 *       {@code VALIDATION_FAILED}).
 * </ul>
 */
package com.maityp394.studentapi.exception;
