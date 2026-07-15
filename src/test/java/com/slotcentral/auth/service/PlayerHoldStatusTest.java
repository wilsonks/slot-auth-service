package com.slotcentral.auth.service;

import com.slotcentral.auth.domain.Player;
import com.slotcentral.auth.domain.PlayerStatus;
import com.slotcentral.auth.dto.player.PlayerResponse;
import com.slotcentral.auth.exception.ResourceNotFoundException;
import com.slotcentral.auth.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerHoldStatusTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(playerRepository, passwordEncoder, jwtService);
    }

    private Player createPlayer(String uid, PlayerStatus status) {
        Player player = new Player();
        player.setUid(uid);
        player.setStatus(status);
        return player;
    }

    @Test
    void shouldToggleActiveToOnHold() {
        Player player = createPlayer("card-001", PlayerStatus.ACTIVE);
        when(playerRepository.findByUid("card-001")).thenReturn(Optional.of(player));
        when(playerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PlayerResponse result = playerService.toggleHold("card-001");
        assertEquals(PlayerStatus.ON_HOLD, result.status());
    }

    @Test
    void shouldToggleOnHoldToActive() {
        Player player = createPlayer("card-001", PlayerStatus.ON_HOLD);
        when(playerRepository.findByUid("card-001")).thenReturn(Optional.of(player));
        when(playerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PlayerResponse result = playerService.toggleHold("card-001");
        assertEquals(PlayerStatus.ACTIVE, result.status());
    }

    @Test
    void shouldThrowWhenPlayerNotFound() {
        when(playerRepository.findByUid("unknown")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> playerService.toggleHold("unknown"));
    }
}
