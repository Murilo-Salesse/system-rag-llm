package github.salessew.notebooklm.controller;

import github.salessew.notebooklm.domain.entity.User;
import github.salessew.notebooklm.dto.SourceStatusResponse;
import github.salessew.notebooklm.dto.SourceUploadResponse;
import github.salessew.notebooklm.exception.ResourceNotFoundException;
import github.salessew.notebooklm.service.SourceService;
import github.salessew.notebooklm.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SourceControllerTest {

    private SourceService sourceService;
    private UserService userService;
    private SourceController sut;

    private Jwt jwt;
    private User currentUser;
    private UUID userId;
    private UUID notebookId;

    @BeforeEach
    void setUp() {
        sourceService = mock(SourceService.class);
        userService = mock(UserService.class);
        sut = new SourceController(sourceService, userService);

        jwt = Jwt.withTokenValue("mock-jwt-token")
                .header("alg", "none")
                .subject("sub-123")
                .build();

        userId = UUID.randomUUID();
        currentUser = new User("sub-123", "user@test.com", "User");
        currentUser.setId(userId);

        notebookId = UUID.randomUUID();
    }

    @Test
    void deve_fazer_upload_de_arquivo_com_sucesso_retornando_202_e_location() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "manual.md", "text/markdown", "content".getBytes());
        UUID sourceId = UUID.randomUUID();
        String statusUrl = "/api/v1/notebooks/" + notebookId + "/sources/" + sourceId;
        SourceUploadResponse response = new SourceUploadResponse(
                sourceId,
                notebookId,
                "manual.md",
                "MARKDOWN",
                "PENDING",
                "Upload aceito com sucesso.",
                statusUrl
        );

        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        when(sourceService.initiateFileUpload(currentUser, notebookId, file, "Custom Name")).thenReturn(response);

        // When
        ResponseEntity<SourceUploadResponse> result = sut.uploadSource(jwt, notebookId, file, "Custom Name");

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(result.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo(statusUrl);
        assertThat(result.getBody()).isEqualTo(response);

        verify(userService).getOrCreateCurrentUser(jwt);
        verify(sourceService).initiateFileUpload(currentUser, notebookId, file, "Custom Name");
    }

    @Test
    void deve_obter_status_da_fonte_com_sucesso() {
        // Given
        UUID sourceId = UUID.randomUUID();
        SourceStatusResponse response = new SourceStatusResponse(
                sourceId,
                notebookId,
                "manual.md",
                "MARKDOWN",
                "READY",
                null,
                Instant.now()
        );

        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        when(sourceService.getSourceStatus(currentUser, notebookId, sourceId)).thenReturn(response);

        // When
        ResponseEntity<SourceStatusResponse> result = sut.getSourceStatus(jwt, notebookId, sourceId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);

        verify(userService).getOrCreateCurrentUser(jwt);
        verify(sourceService).getSourceStatus(currentUser, notebookId, sourceId);
    }

    @Test
    void deve_lancar_404_quando_notebook_de_outro_usuario_no_upload() {
        MockMultipartFile file = new MockMultipartFile("file", "manual.md", "text/markdown", "content".getBytes());
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        when(sourceService.initiateFileUpload(currentUser, notebookId, file, null))
                .thenThrow(new ResourceNotFoundException("Notebook não encontrado"));

        assertThatThrownBy(() -> sut.uploadSource(jwt, notebookId, file, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notebook não encontrado");
    }

    @Test
    void deve_lancar_404_quando_fonte_nao_encontrada_ao_consultar_status() {
        UUID sourceId = UUID.randomUUID();
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        when(sourceService.getSourceStatus(currentUser, notebookId, sourceId))
                .thenThrow(new ResourceNotFoundException("Fonte não encontrada no notebook."));

        assertThatThrownBy(() -> sut.getSourceStatus(jwt, notebookId, sourceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Fonte não encontrada no notebook.");
    }
}
