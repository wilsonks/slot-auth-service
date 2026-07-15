package com.slotcentral.auth.service;

import com.slotcentral.auth.domain.Player;
import com.slotcentral.auth.domain.PlayerStatus;
import com.slotcentral.auth.dto.player.*;
import com.slotcentral.auth.exception.PlayerOnHoldException;
import com.slotcentral.auth.exception.ResourceNotFoundException;
import com.slotcentral.auth.repository.PlayerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;

@Service
@Transactional
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public PlayerService(PlayerRepository playerRepository,
                         PasswordEncoder passwordEncoder,
                         JwtService jwtService) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public PlayerSessionResponse startSession(PlayerSessionRequest request) {
        Player player = playerRepository.findByUid(request.uid())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid Card: " + request.uid()));

        if (player.getStatus() == PlayerStatus.ON_HOLD) {
            throw new PlayerOnHoldException(request.uid());
        }

        String token = jwtService.issuePlayerToken(player.getUid());
        return new PlayerSessionResponse(
                token,
                player.getUid(),
                player.getStatus().name(),
                jwtService.getPlayerSessionTtlSeconds()
        );
    }

    public void closeSession(String uid, String bearerToken) throws ParseException {
        findByUid(uid);
        jwtService.revokeToken(bearerToken);
    }

    @Transactional(readOnly = true)
    public PlayerResponse getPlayer(String uid) {
        return PlayerResponse.from(findByUid(uid));
    }

    public PlayerResponse updateNickname(String uid, UpdateNicknameRequest request) {
        Player player = findByUid(uid);
        player.setNickname(request.nickname());
        return PlayerResponse.from(playerRepository.save(player));
    }

    public PlayerResponse updatePin(String uid, UpdatePinRequest request) {
        Player player = findByUid(uid);
        player.setPinHash(passwordEncoder.encode(request.pin()));
        return PlayerResponse.from(playerRepository.save(player));
    }

    public PlayerResponse toggleHold(String uid) {
        Player player = findByUid(uid);
        if (player.getStatus() == PlayerStatus.ACTIVE) {
            player.setStatus(PlayerStatus.ON_HOLD);
        } else {
            player.setStatus(PlayerStatus.ACTIVE);
        }
        return PlayerResponse.from(playerRepository.save(player));
    }

    private Player findByUid(String uid) {
        return playerRepository.findByUid(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + uid));
    }
}
