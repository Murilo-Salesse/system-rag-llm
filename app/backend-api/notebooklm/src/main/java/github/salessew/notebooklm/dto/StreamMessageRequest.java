package github.salessew.notebooklm.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record StreamMessageRequest(
        @NotBlank(message = "O conteúdo da mensagem não pode ser vazio")
        String content,
        List<UUID> activeSourceIds
) {
    public StreamMessageRequest {
        activeSourceIds = activeSourceIds != null ? List.copyOf(activeSourceIds) : List.of();
    }
}
