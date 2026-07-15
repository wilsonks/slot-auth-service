package com.slotcentral.auth.dto.player;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
    @NotBlank @Size(max = 100) String nickname
) {}
