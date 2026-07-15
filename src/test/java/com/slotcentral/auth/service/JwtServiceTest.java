package com.slotcentral.auth.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.slotcentral.auth.domain.EmployeeRole;
import com.slotcentral.auth.repository.RevokedTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtServiceTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService("", "", revokedTokenRepository);
        // @Value fields are not injected outside Spring context; set them explicitly
        ReflectionTestUtils.setField(jwtService, "playerSessionTtlSeconds", 3600L);
        ReflectionTestUtils.setField(jwtService, "employeeTokenTtlSeconds", 28800L);
        when(revokedTokenRepository.existsByJti(anyString())).thenReturn(false);
    }

    @Test
    void shouldIssueAndValidatePlayerToken() throws Exception {
        String token = jwtService.issuePlayerToken("player-001");
        assertNotNull(token);
        JWTClaimsSet claims = jwtService.validateToken(token);
        assertEquals("player-001", claims.getSubject());
        assertEquals("player", claims.getStringClaim("type"));
    }

    @Test
    void shouldIssueAndValidateEmployeeToken() throws Exception {
        String token = jwtService.issueEmployeeToken("emp-001", EmployeeRole.ADMIN);
        assertNotNull(token);
        JWTClaimsSet claims = jwtService.validateToken(token);
        assertEquals("emp-001", claims.getSubject());
        assertEquals("employee", claims.getStringClaim("type"));
        assertEquals("ADMIN", claims.getStringClaim("role"));
    }

    @Test
    void shouldExposePublicKeyOnlyInJwks() {
        var jwks = jwtService.getJwks();
        assertNotNull(jwks);
        assertFalse(jwks.getKeys().isEmpty());
        // Should be an RSA public key - verify it has no private key material
        var key = jwks.getKeys().get(0);
        assertNotNull(key.toRSAKey());
        assertFalse(key.toRSAKey().isPrivate(), "JWKS must not expose private key material");
    }

    @Test
    void shouldRevokeToken() throws Exception {
        String token = jwtService.issuePlayerToken("player-002");
        jwtService.revokeToken(token);
        // Simulate the revocation being persisted
        when(revokedTokenRepository.existsByJti(anyString())).thenReturn(true);
        assertThrows(Exception.class, () -> jwtService.validateToken(token));
    }
}
