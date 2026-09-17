package github.salessew.notebooklm.dto;

import java.time.Instant;
import java.util.UUID;

public record SourceStatusResponse(
        UUID id,
        UUID notebookId,
        String name,
        String type,
        String status,
        String errorMessage,
        Instant createdAt
) {}
