# Student Management REST API

A production-ready, secure RESTful API for student management built with **Spring Boot 4**, **Java 25**, **Spring Security 6+ (OAuth2 Resource Server with JWT)**, **Spring Data JPA**, and **PostgreSQL**.

## Features

- **Authentication & Authorization**:
  - Stateless JWT Bearer token authentication via Spring Security's native OAuth2 Resource Server.
  - User registration (`/api/v1/auth/register`) with BCrypt password hashing (`$2a$`).
  - Secure login (`/api/v1/auth/login`) issuing signed HMAC-SHA256 JWT tokens.
  - Identity endpoint (`/api/v1/auth/me`) resolving user profile from `@AuthenticationPrincipal Jwt`.
  - Stateless logout (`/api/v1/auth/logout`) clearing security context.
- **Student CRUD Operations**:
  - Paginated, sorted, and filtered student listings (`/api/v1/students`).
  - Retrieve student by UUID (`GET /api/v1/students/{id}`).
  - Full update (`PUT /api/v1/students/{id}`) and partial update (`PATCH /api/v1/students/{id}`).
  - Deletion (`DELETE /api/v1/students/{id}`).
- **RFC 9457 Problem Details Error Handling**:
  - Structured, machine-readable error responses for validation failures, conflicts, bad credentials, type mismatches, and not-found states.
- **Request Tracing & Logging**:
  - Automatic `X-Request-ID` generation and propagation for distributed tracing.
  - Formatted request duration and status logging.
- **Code Quality & Consistency**:
  - Google Java Format strictly enforced using the **Spotless Maven Plugin**.
  - Automated Git pre-commit hooks (`.githooks`).

## Tech Stack

| Technology                        | Purpose                                                |
| :-------------------------------- | :----------------------------------------------------- |
| **Java 25**                       | Core runtime                                           |
| **Spring Boot 4.1.1**             | Application framework                                  |
| **Spring Security**               | Security and authentication foundation                 |
| **Spring OAuth2 Resource Server** | Native JWT validation and Bearer token filter pipeline |
| **Spring Data JPA & Hibernate**   | ORM and database persistence                           |
| **PostgreSQL**                    | Primary relational database                            |
| **Nimbus JOSE + JWT**             | Cryptographic JWT signing and decoding                 |
| **Lombok**                        | Boilerplate reduction                                  |
| **Spotless (Google Java Format)** | Code formatting enforcement                            |

## Project Structure

```
d:/REST_API/
├── .env                                # Local environment variables
├── .githooks/                          # Git hooks (pre-commit spotless checks)
├── api-test.http                       # Interactive HTTP client test script
├── pom.xml                             # Maven project dependencies and build configuration
└── src/
    ├── main/
    │   ├── java/com/maityp394/studentapi/
    │   │   ├── controller/             # REST API Controllers (AuthController, StudentController)
    │   │   ├── dto/                    # Data Transfer Objects
    │   │   │   ├── request/            # Request DTOs (RegisterRequest, LoginRequest, etc.)
    │   │   │   └── response/           # Response DTOs (ApiResponse, AuthResponse, StudentResponse)
    │   │   ├── entity/                 # JPA Entities (Student, Responsibility)
    │   │   ├── exception/              # RFC 9457 Global Exception Handling
    │   │   ├── filter/                 # Request logging and X-Request-ID filter
    │   │   ├── mapper/                 # Entity <-> DTO mappers (StudentMapper)
    │   │   ├── repository/             # Spring Data JPA Repositories
    │   │   ├── security/               # Security config, JWT service, UserDetailsService
    │   │   └── service/                # Business logic interfaces & implementations
    │   └── resources/
    │       └── application.properties  # Application properties
    └── test/                           # Full integration test suite
```

## Getting Started

### Prerequisites

- **Java 25 JDK** installed and configured (`JAVA_HOME`).
- **Git**.
- Access to a **PostgreSQL** instance (e.g. Neon, local PostgreSQL, or Docker).

### Configuration (`.env`)

For local development, copy the provided `.env.example` template to `.env`:

```bash
cp .env.example .env
```

Customize the values in `.env`:

```properties
# Database connection string
DATABASE_URL=jdbc:postgresql://<host>:<port>/<dbname>?user=<username>&password=<password>&sslmode=require

# 256-bit Base64-encoded secret key for signing JWTs
JWT_SECRET=your-base64-encoded-256-bit-secret-key-here

# Active Spring Profile (dev for local development)
SPRING_PROFILES_ACTIVE=dev
```

For production deployments, consult [.env.production](.env.production) and configure the environment variables in your deployment platform (Render, Railway, AWS, Docker, Kubernetes) with `SPRING_PROFILES_ACTIVE=prod`.

> **Note**: To generate a valid 256-bit Base64 secret key:
>
> ```bash
> openssl rand -base64 32
> ```

## Running the Application

Using the Maven wrapper:

**Windows (PowerShell/CMD):**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
./mvnw spring-boot:run
```

The application runs by default on `http://localhost:8000`.

## API Reference

### 1. Authentication Endpoints

| Method | Endpoint                | Auth Required       | Description                                                                |
| :----- | :---------------------- | :------------------ | :------------------------------------------------------------------------- |
| `POST` | `/api/v1/auth/register` | No                  | Register a new student account (returns `201 Created` + `Location` header) |
| `POST` | `/api/v1/auth/login`    | No                  | Authenticate credentials and receive `HttpOnly` cookie `access_token`      |
| `GET`  | `/api/v1/auth/me`       | Cookie / Bearer     | Retrieve profile information for the authenticated user                    |
| `POST` | `/api/v1/auth/logout`   | No / Token / Cookie | Clear `access_token` cookie (`Max-Age=0`) and security context             |

#### Sample Register Request

```json
POST /api/v1/auth/register
Content-Type: application/json

{
  "name": "Jane Doe",
  "email": "jane.doe@example.com",
  "password": "SecurePassword123!"
}
```

#### Sample Login Request

```json
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "jane.doe@example.com",
  "password": "SecurePassword123!"
}
```

#### Sample Login Response

```http
HTTP/1.1 200 OK
Set-Cookie: access_token=eyJhbGciOiJIUzI1NiIs...; Path=/; Max-Age=900; HttpOnly; SameSite=Lax
Content-Type: application/json

{
  "message": "Login successful",
  "data": {
    "id": "c1f728ea-8b43-4dc9-983b-e10ebaf325d7",
    "name": "Jane Doe",
    "email": "jane.doe@example.com",
    "createdAt": "2026-09-07T10:30:00Z",
    "updatedAt": "2026-09-07T10:30:00Z"
  },
  "timestamp": "2026-09-07T10:30:00Z"
}
```

> **Security Note on Token Storage**: The JWT `access_token` is transmitted exclusively as an `HttpOnly`, `SameSite=Lax` cookie (`Secure` in production) rather than in the JSON response body. This prevents token theft via Cross-Site Scripting (XSS).

### Dual-Transport Authentication & CSRF Protection

The API supports dual-transport token resolution:
1. **Header-first**: `Authorization: Bearer <token>` header (prioritized, exempt from CSRF checks — ideal for native apps, CLI, and cURL).
2. **Cookie fallback**: `access_token` `HttpOnly` cookie (ideal for browser/SPA clients).
3. **CSRF Protection**: Stateful mutating requests (`POST`, `PUT`, `PATCH`, `DELETE`) authenticated via cookies require an `X-XSRF-TOKEN` header matching the readable `XSRF-TOKEN` cookie (SPA CSRF Double Submit pattern). Safe read requests (`GET`, `HEAD`) and public auth endpoints are exempt.

### 2. Student Resource Endpoints (Protected)

All student endpoints require authentication via either the `access_token` cookie or the `Authorization: Bearer <token>` header. Mutating requests using cookie authentication also require the `X-XSRF-TOKEN` header.

| Method   | Endpoint                                        | Description                                  |
| :------- | :---------------------------------------------- | :------------------------------------------- |
| `GET`    | `/api/v1/students?page=0&size=10&sort=name,asc` | Get paginated list of students               |
| `GET`    | `/api/v1/students/{id}`                         | Get student details by UUID                  |
| `PUT`    | `/api/v1/students/{id}`                         | Full update of student details (name, email) |
| `PATCH`  | `/api/v1/students/{id}`                         | Partial update of student details            |
| `DELETE` | `/api/v1/students/{id}`                         | Delete student by UUID (`204 No Content`)    |

## Testing

### Automated Test Suite

Run the full integration test suite covering registration, login, protected endpoint access, validation, and error handling:

```powershell
.\mvnw.cmd test
```

### Interactive Testing (`api-test.http`)

An [api-test.http](api-test.http) file is included at the project root for testing with the **REST Client** extension in VS Code or **IntelliJ HTTP Client**.

1. Run **2.1 Register** to create a student.
2. Run **2.2 Login** to automatically capture the JWT token into `@authToken`.
3. Run any of the student endpoints or error scenarios — all requests automatically use the captured `@authToken` and `@studentId`.

## Code Quality & Formatting

The project enforces **Google Java Format** via Spotless:

- **Verify formatting**:
  ```powershell
  .\mvnw.cmd spotless:check
  ```
- **Automatically format code**:
  ```powershell
  .\mvnw.cmd spotless:apply
  ```
- **Git Hooks**: Pre-commit hooks are configured via `.githooks` to automatically check formatting before commits are created.
