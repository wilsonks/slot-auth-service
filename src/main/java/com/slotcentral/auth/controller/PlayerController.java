package com.slotcentral.auth.controller;

import com.slotcentral.auth.dto.player.*;
import com.slotcentral.auth.service.PlayerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/session")
    public ResponseEntity<PlayerSessionResponse> startSession(@Valid @RequestBody PlayerSessionRequest request) {
        return ResponseEntity.ok(playerService.startSession(request));
    }

    @PostMapping("/{uid}/session/close")
    @PreAuthorize("hasRole('PLAYER') or hasAnyRole('ADMIN', 'FLOOR_MANAGER')")
    public ResponseEntity<Void> closeSession(@PathVariable String uid, HttpServletRequest request) throws ParseException {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : "";
        playerService.closeSession(uid, token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{uid}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PlayerResponse> getPlayer(@PathVariable String uid) {
        return ResponseEntity.ok(playerService.getPlayer(uid));
    }

    @PutMapping("/{uid}/nickname")
    @PreAuthorize("hasRole('PLAYER') or hasAnyRole('ADMIN', 'FLOOR_MANAGER')")
    public ResponseEntity<PlayerResponse> updateNickname(
            @PathVariable String uid,
            @Valid @RequestBody UpdateNicknameRequest request) {
        return ResponseEntity.ok(playerService.updateNickname(uid, request));
    }

    @PutMapping("/{uid}/pin")
    @PreAuthorize("hasRole('PLAYER') or hasRole('ADMIN')")
    public ResponseEntity<PlayerResponse> updatePin(
            @PathVariable String uid,
            @Valid @RequestBody UpdatePinRequest request) {
        return ResponseEntity.ok(playerService.updatePin(uid, request));
    }

    @PutMapping("/{uid}/hold")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLOOR_MANAGER')")
    public ResponseEntity<PlayerResponse> toggleHold(@PathVariable String uid) {
        return ResponseEntity.ok(playerService.toggleHold(uid));
    }
}
