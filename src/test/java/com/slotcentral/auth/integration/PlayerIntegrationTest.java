package com.slotcentral.auth.integration;

import com.slotcentral.auth.domain.Player;
import com.slotcentral.auth.domain.PlayerStatus;
import com.slotcentral.auth.dto.employee.EmployeeLoginRequest;
import com.slotcentral.auth.dto.employee.EmployeeLoginResponse;
import com.slotcentral.auth.dto.player.*;
import com.slotcentral.auth.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

class PlayerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PlayerRepository playerRepository;

    private Player testPlayer;

    @BeforeEach
    void setUp() {
        playerRepository.deleteAll();
        testPlayer = new Player();
        testPlayer.setUid("card-test-001");
        testPlayer.setNickname("TestPlayer");
        testPlayer.setStatus(PlayerStatus.ACTIVE);
        playerRepository.save(testPlayer);
    }

    private String adminToken() {
        EmployeeLoginRequest req = new EmployeeLoginRequest("admin", "Admin@1234");
        ResponseEntity<EmployeeLoginResponse> resp = restTemplate.postForEntity(
                "/api/v1/employees/login", req, EmployeeLoginResponse.class);
        return resp.getBody().token();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void shouldStartPlayerSession() {
        PlayerSessionRequest req = new PlayerSessionRequest("card-test-001");
        ResponseEntity<PlayerSessionResponse> resp = restTemplate.postForEntity(
                "/api/v1/players/session", req, PlayerSessionResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody().token());
        assertEquals("card-test-001", resp.getBody().uid());
    }

    @Test
    void shouldRejectOnHoldCard() {
        testPlayer.setStatus(PlayerStatus.ON_HOLD);
        playerRepository.save(testPlayer);

        PlayerSessionRequest req = new PlayerSessionRequest("card-test-001");
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/v1/players/session", req, String.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void shouldRejectUnknownCard() {
        PlayerSessionRequest req = new PlayerSessionRequest("card-unknown");
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/v1/players/session", req, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void shouldUpdateNickname() {
        String token = adminToken();
        UpdateNicknameRequest req = new UpdateNicknameRequest("NewNick");
        ResponseEntity<PlayerResponse> resp = restTemplate.exchange(
                "/api/v1/players/card-test-001/nickname", HttpMethod.PUT,
                new HttpEntity<>(req, authHeaders(token)), PlayerResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("NewNick", resp.getBody().nickname());
    }

    @Test
    void shouldToggleHold() {
        String token = adminToken();
        ResponseEntity<PlayerResponse> resp1 = restTemplate.exchange(
                "/api/v1/players/card-test-001/hold", HttpMethod.PUT,
                new HttpEntity<>(authHeaders(token)), PlayerResponse.class);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());
        assertEquals(PlayerStatus.ON_HOLD, resp1.getBody().status());

        ResponseEntity<PlayerResponse> resp2 = restTemplate.exchange(
                "/api/v1/players/card-test-001/hold", HttpMethod.PUT,
                new HttpEntity<>(authHeaders(token)), PlayerResponse.class);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
        assertEquals(PlayerStatus.ACTIVE, resp2.getBody().status());
    }

    @Test
    void shouldCloseSession() {
        PlayerSessionRequest sessionReq = new PlayerSessionRequest("card-test-001");
        ResponseEntity<PlayerSessionResponse> sessionResp = restTemplate.postForEntity(
                "/api/v1/players/session", sessionReq, PlayerSessionResponse.class);
        String token = sessionResp.getBody().token();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Void> closeResp = restTemplate.exchange(
                "/api/v1/players/card-test-001/session/close", HttpMethod.POST,
                new HttpEntity<>(headers), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, closeResp.getStatusCode());
    }
}
