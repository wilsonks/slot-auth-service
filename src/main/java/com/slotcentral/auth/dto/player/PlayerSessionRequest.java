package com.slotcentral.auth.dto.player;

import jakarta.validation.constraints.NotBlank;

public record PlayerSessionRequest(
    @NotBlank String uid
) {}
