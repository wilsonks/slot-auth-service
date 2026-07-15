package com.slotcentral.auth.dto.player;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePinRequest(
    @NotBlank @Size(min = 4, max = 10) String pin
) {}
