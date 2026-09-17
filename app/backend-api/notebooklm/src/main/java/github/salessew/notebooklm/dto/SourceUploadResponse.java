package github.salessew.notebooklm.dto;

import java.util.UUID;

public record SourceUploadResponse(
        UUID sourceId,
        UUID notebookId,
        String name,
        String type,
        String status,
        String message,
        String statusUrl
) {}
