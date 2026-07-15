package com.slotcentral.auth.integration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.slotcentral.auth.dto.employee.EmployeeLoginRequest;
import com.slotcentral.auth.dto.employee.EmployeeLoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwksIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldReturnValidJwks() {
        ResponseEntity<Map> resp = restTemplate.getForEntity("/.well-known/jwks.json", Map.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("keys"));
    }

    @Test
    void shouldVerifyEmployeeTokenAgainstJwks() throws Exception {
        EmployeeLoginRequest loginReq = new EmployeeLoginRequest("admin", "Admin@1234");
        ResponseEntity<EmployeeLoginResponse> loginResp = restTemplate.postForEntity(
                "/api/v1/employees/login", loginReq, EmployeeLoginResponse.class);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());
        String token = loginResp.getBody().token();

        ResponseEntity<String> jwksResp = restTemplate.getForEntity("/.well-known/jwks.json", String.class);
        assertEquals(HttpStatus.OK, jwksResp.getStatusCode());

        JWKSet jwkSet = JWKSet.parse(jwksResp.getBody());

        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(
                JWSAlgorithm.RS256, new ImmutableJWKSet<>(jwkSet)));
        JWTClaimsSet claims = processor.process(token, null);

        assertEquals("admin", claims.getSubject());
        assertEquals("employee", claims.getStringClaim("type"));
        assertEquals("ADMIN", claims.getStringClaim("role"));
    }
}
