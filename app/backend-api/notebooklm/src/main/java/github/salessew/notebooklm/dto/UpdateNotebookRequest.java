package github.salessew.notebooklm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNotebookRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 1, max = 255, message = "O nome deve ter entre 1 e 255 caracteres")
        String name,

        @Size(max = 1000, message = "A descrição não pode exceder 1000 caracteres")
        String description
) {
}
