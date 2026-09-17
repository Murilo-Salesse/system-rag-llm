package github.salessew.notebooklm.dto;

import java.time.Instant;
import java.util.UUID;

public record NotebookResponse(
        UUID id,
        UUID ownerId,
        String name,
        String description,
        long sourceCount,
        Instant createdAt,
        Instant updatedAt
) {
}
