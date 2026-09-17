package github.salessew.notebooklm.controller;

import github.salessew.notebooklm.domain.entity.User;
import github.salessew.notebooklm.dto.CreateNotebookRequest;
import github.salessew.notebooklm.dto.NotebookResponse;
import github.salessew.notebooklm.dto.UpdateNotebookRequest;
import github.salessew.notebooklm.exception.ResourceNotFoundException;
import github.salessew.notebooklm.service.NotebookService;
import github.salessew.notebooklm.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class NotebookControllerTest {

    private NotebookService notebookService;
    private UserService userService;
    private NotebookController sut;

    private Jwt jwt;
    private User currentUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        notebookService = mock(NotebookService.class);
        userService = mock(UserService.class);
        sut = new NotebookController(notebookService, userService);

        jwt = Jwt.withTokenValue("mock-jwt-token")
                .header("alg", "none")
                .subject("sub-123")
                .build();

        userId = UUID.randomUUID();
        currentUser = new User("sub-123", "user@test.com", "User");
        currentUser.setId(userId);
    }

    @Test
    void deve_listar_notebooks_com_sucesso() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        NotebookResponse response = new NotebookResponse(
                UUID.randomUUID(), userId, "Notebook 1", "Desc 1", 2L, Instant.now(), Instant.now()
        );
        when(notebookService.listNotebooks(currentUser, pageable))
                .thenReturn(new PageImpl<>(List.of(response)));

        // When
        ResponseEntity<Page<NotebookResponse>> result = sut.listNotebooks(jwt, pageable);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(1);
        verify(userService).getOrCreateCurrentUser(jwt);
        verify(notebookService).listNotebooks(currentUser, pageable);
    }

    @Test
    void deve_criar_notebook_com_sucesso() {
        // Given
        CreateNotebookRequest request = new CreateNotebookRequest("Novo", "Desc");
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        NotebookResponse response = new NotebookResponse(
                UUID.randomUUID(), userId, "Novo", "Desc", 0L, Instant.now(), Instant.now()
        );
        when(notebookService.createNotebook(currentUser, request)).thenReturn(response);

        // When
        ResponseEntity<NotebookResponse> result = sut.createNotebook(jwt, request);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().name()).isEqualTo("Novo");
        verify(userService).getOrCreateCurrentUser(jwt);
        verify(notebookService).createNotebook(currentUser, request);
    }

    @Test
    void deve_obter_notebook_por_id_com_sucesso() {
        // Given
        UUID notebookId = UUID.randomUUID();
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        NotebookResponse response = new NotebookResponse(
                notebookId, userId, "Notebook", "Desc", 1L, Instant.now(), Instant.now()
        );
        when(notebookService.getNotebook(currentUser, notebookId)).thenReturn(response);

        // When
        ResponseEntity<NotebookResponse> result = sut.getNotebook(jwt, notebookId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(userService).getOrCreateCurrentUser(jwt);
        verify(notebookService).getNotebook(currentUser, notebookId);
    }

    @Test
    void deve_lancar_404_ao_obter_notebook_de_outro_usuario() {
        // Given
        UUID notebookId = UUID.randomUUID();
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        when(notebookService.getNotebook(currentUser, notebookId))
                .thenThrow(new ResourceNotFoundException("Notebook não encontrado"));

        // When & Then
        assertThatThrownBy(() -> sut.getNotebook(jwt, notebookId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notebook não encontrado");
        verify(userService).getOrCreateCurrentUser(jwt);
        verify(notebookService).getNotebook(currentUser, notebookId);
    }

    @Test
    void deve_atualizar_notebook_com_sucesso() {
        // Given
        UUID notebookId = UUID.randomUUID();
        UpdateNotebookRequest request = new UpdateNotebookRequest("Atualizado", "Desc Atualizada");
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        NotebookResponse response = new NotebookResponse(
                notebookId, userId, "Atualizado", "Desc Atualizada", 1L, Instant.now(), Instant.now()
        );
        when(notebookService.updateNotebook(currentUser, notebookId, request)).thenReturn(response);

        // When
        ResponseEntity<NotebookResponse> result = sut.updateNotebook(jwt, notebookId, request);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(userService).getOrCreateCurrentUser(jwt);
        verify(notebookService).updateNotebook(currentUser, notebookId, request);
    }

    @Test
    void deve_lancar_404_ao_atualizar_notebook_de_outro_usuario() {
        // Given
        UUID notebookId = UUID.randomUUID();
        UpdateNotebookRequest request = new UpdateNotebookRequest("Atualizado", "Desc");
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        when(notebookService.updateNotebook(currentUser, notebookId, request))
                .thenThrow(new ResourceNotFoundException("Notebook não encontrado"));

        // When & Then
        assertThatThrownBy(() -> sut.updateNotebook(jwt, notebookId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notebook não encontrado");
        verify(userService).getOrCreateCurrentUser(jwt);
        verify(notebookService).updateNotebook(currentUser, notebookId, request);
    }

    @Test
    void deve_excluir_notebook_com_sucesso() {
        // Given
        UUID notebookId = UUID.randomUUID();
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        doNothing().when(notebookService).deleteNotebook(currentUser, notebookId);

        // When
        ResponseEntity<Void> result = sut.deleteNotebook(jwt, notebookId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).getOrCreateCurrentUser(jwt);
        verify(notebookService).deleteNotebook(currentUser, notebookId);
    }

    @Test
    void deve_lancar_404_ao_excluir_notebook_de_outro_usuario() {
        // Given
        UUID notebookId = UUID.randomUUID();
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        doThrow(new ResourceNotFoundException("Notebook não encontrado"))
                .when(notebookService).deleteNotebook(currentUser, notebookId);

        // When & Then
        assertThatThrownBy(() -> sut.deleteNotebook(jwt, notebookId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notebook não encontrado");
        verify(userService).getOrCreateCurrentUser(jwt);
        verify(notebookService).deleteNotebook(currentUser, notebookId);
    }
}
