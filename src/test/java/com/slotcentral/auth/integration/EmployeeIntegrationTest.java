package com.slotcentral.auth.integration;

import com.slotcentral.auth.domain.EmployeeRole;
import com.slotcentral.auth.dto.employee.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String adminToken() {
        EmployeeLoginRequest req = new EmployeeLoginRequest("admin", "Admin@1234");
        ResponseEntity<EmployeeLoginResponse> resp = restTemplate.postForEntity(
                "/api/v1/employees/login", req, EmployeeLoginResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return resp.getBody().token();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void shouldLoginAsDefaultAdmin() {
        EmployeeLoginRequest req = new EmployeeLoginRequest("admin", "Admin@1234");
        ResponseEntity<EmployeeLoginResponse> resp = restTemplate.postForEntity(
                "/api/v1/employees/login", req, EmployeeLoginResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody().token());
        assertEquals("ADMIN", resp.getBody().role());
        assertTrue(resp.getBody().forcePasswordChange());
    }

    @Test
    void shouldCreateAndGetEmployee() {
        String token = adminToken();
        CreateEmployeeRequest create = new CreateEmployeeRequest(
                "emp-test-001", "Test Employee", EmployeeRole.TECHNICIAN, "Secure123!");

        ResponseEntity<EmployeeResponse> created = restTemplate.exchange(
                "/api/v1/employees", HttpMethod.POST,
                new HttpEntity<>(create, authHeaders(token)), EmployeeResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertEquals("emp-test-001", created.getBody().uid());

        ResponseEntity<EmployeeResponse> fetched = restTemplate.exchange(
                "/api/v1/employees/emp-test-001", HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), EmployeeResponse.class);
        assertEquals(HttpStatus.OK, fetched.getStatusCode());
        assertEquals("Test Employee", fetched.getBody().name());
    }

    @Test
    void shouldReturn404ForUnknownEmployee() {
        String token = adminToken();
        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/v1/employees/nonexistent", HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void shouldDeleteEmployee() {
        String token = adminToken();
        CreateEmployeeRequest create = new CreateEmployeeRequest(
                "emp-to-delete", "Delete Me", EmployeeRole.CAGE_CASHIER, "Password1!");
        restTemplate.exchange("/api/v1/employees", HttpMethod.POST,
                new HttpEntity<>(create, authHeaders(token)), EmployeeResponse.class);

        ResponseEntity<Void> del = restTemplate.exchange(
                "/api/v1/employees/emp-to-delete", HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, del.getStatusCode());

        ResponseEntity<String> get = restTemplate.exchange(
                "/api/v1/employees/emp-to-delete", HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), String.class);
        assertEquals(HttpStatus.NOT_FOUND, get.getStatusCode());
    }
}
