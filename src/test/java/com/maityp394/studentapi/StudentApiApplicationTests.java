package com.maityp394.studentapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.maityp394.studentapi.dto.request.CreateStudentRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class StudentApiApplicationTests {

	@Autowired
	TestRestTemplate testRestTemplate;

	@Test
	void shouldReturnStudentWhenValidIdIsProvided() {
		CreateStudentRequest newStudent = new CreateStudentRequest("John Doe", "john.doe@example.com", "Secret123!");
		ResponseEntity<String> createResponse = testRestTemplate.postForEntity("/api/v1/students", newStudent,
				String.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		DocumentContext createContext = JsonPath.parse(createResponse.getBody());
		String id = createContext.read("$.data.id");
		assertThat(id).isNotBlank();

		ResponseEntity<String> response = testRestTemplate.getForEntity("/api/v1/students/" + id, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		String name = documentContext.read("$.data.name");
		String email = documentContext.read("$.data.email");

		assertThat(name).isEqualTo("John Doe");
		assertThat(email).isEqualTo("john.doe@example.com");
	}

	@Test
	void shouldReturnNotFoundWhenInvalidIdIsProvided() {
		ResponseEntity<String> response = testRestTemplate.getForEntity("/api/v1/students/999", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		String message = documentContext.read("$.message");
		assertThat(message).isEqualTo("Student not found");
	}

	@Test
	void shouldCreateNewStudent() {
		CreateStudentRequest newStudent = new CreateStudentRequest("Jane Doe", "jane.doe@example.com", "Secret123!");
		ResponseEntity<String> response = testRestTemplate.postForEntity("/api/v1/students", newStudent, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI locationOfNewStudent = response.getHeaders().getLocation();
		assertThat(locationOfNewStudent).isNotNull();
		ResponseEntity<String> getResponse = testRestTemplate.getForEntity(locationOfNewStudent, String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
}
