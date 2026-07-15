package com.slotcentral.auth.dto.player;

public record PlayerSessionResponse(
    String token,
    String uid,
    String status,
    long expiresIn
) {}
