package github.salessew.notebooklm.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        String role,
        String content,
        Instant createdAt
) {
}
