package com.slotcentral.auth.dto.player;

import com.slotcentral.auth.domain.Player;
import com.slotcentral.auth.domain.PlayerStatus;
import java.time.Instant;

public record PlayerResponse(
    Long id,
    String uid,
    String nickname,
    PlayerStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static PlayerResponse from(Player p) {
        return new PlayerResponse(
            p.getId(), p.getUid(), p.getNickname(), p.getStatus(),
            p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
