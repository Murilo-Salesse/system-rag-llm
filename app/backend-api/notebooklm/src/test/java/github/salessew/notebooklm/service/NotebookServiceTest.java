package github.salessew.notebooklm.service;

import github.salessew.notebooklm.domain.entity.Notebook;
import github.salessew.notebooklm.domain.entity.User;
import github.salessew.notebooklm.domain.enums.SourceStatus;
import github.salessew.notebooklm.domain.repository.NotebookRepository;
import github.salessew.notebooklm.domain.repository.SourceRepository;
import github.salessew.notebooklm.dto.CreateNotebookRequest;
import github.salessew.notebooklm.dto.NotebookResponse;
import github.salessew.notebooklm.dto.UpdateNotebookRequest;
import github.salessew.notebooklm.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotebookServiceTest {

    private NotebookRepository notebookRepository;
    private SourceRepository sourceRepository;
    private NotebookService sut;

    private User currentUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        notebookRepository = mock(NotebookRepository.class);
        sourceRepository = mock(SourceRepository.class);
        sut = new NotebookService(notebookRepository, sourceRepository);

        userId = UUID.randomUUID();
        currentUser = new User("sub-1", "user@test.com", "Test User");
        currentUser.setId(userId);
    }

    @Test
    void deve_listar_notebooks_com_paginacao_e_contagem_de_fontes() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Notebook notebook = new Notebook(currentUser, "Notebook 1", "Desc 1");
        UUID notebookId = UUID.randomUUID();
        notebook.setId(notebookId);
        notebook.setCreatedAt(Instant.now());
        notebook.setUpdatedAt(Instant.now());

        when(notebookRepository.findByOwnerId(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(notebook)));
        when(sourceRepository.countByNotebookIdAndStatus(notebookId, SourceStatus.READY))
                .thenReturn(3L);

        // When
        Page<NotebookResponse> result = sut.listNotebooks(currentUser, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        NotebookResponse response = result.getContent().get(0);
        assertThat(response.id()).isEqualTo(notebookId);
        assertThat(response.ownerId()).isEqualTo(userId);
        assertThat(response.name()).isEqualTo("Notebook 1");
        assertThat(response.description()).isEqualTo("Desc 1");
        assertThat(response.sourceCount()).isEqualTo(3L);
        verify(notebookRepository).findByOwnerId(userId, pageable);
        verify(sourceRepository).countByNotebookIdAndStatus(notebookId, SourceStatus.READY);
    }

    @Test
    void deve_criar_notebook_com_sucesso() {
        // Given
        CreateNotebookRequest request = new CreateNotebookRequest("Novo Notebook", "Nova Descrição");
        Notebook savedNotebook = new Notebook(currentUser, "Novo Notebook", "Nova Descrição");
        UUID notebookId = UUID.randomUUID();
        savedNotebook.setId(notebookId);
        savedNotebook.setCreatedAt(Instant.now());
        savedNotebook.setUpdatedAt(Instant.now());

        when(notebookRepository.save(any(Notebook.class))).thenReturn(savedNotebook);
        when(sourceRepository.countByNotebookIdAndStatus(notebookId, SourceStatus.READY)).thenReturn(0L);

        // When
        NotebookResponse result = sut.createNotebook(currentUser, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(notebookId);
        assertThat(result.name()).isEqualTo("Novo Notebook");
        assertThat(result.description()).isEqualTo("Nova Descrição");
        assertThat(result.sourceCount()).isZero();
        verify(notebookRepository).save(any(Notebook.class));
    }

    @Test
    void deve_obter_notebook_por_id_com_sucesso() {
        // Given
        UUID notebookId = UUID.randomUUID();
        Notebook notebook = new Notebook(currentUser, "Meu Notebook", "Desc");
        notebook.setId(notebookId);
        notebook.setCreatedAt(Instant.now());
        notebook.setUpdatedAt(Instant.now());

        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.of(notebook));
        when(sourceRepository.countByNotebookIdAndStatus(notebookId, SourceStatus.READY)).thenReturn(2L);

        // When
        NotebookResponse result = sut.getNotebook(currentUser, notebookId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(notebookId);
        assertThat(result.sourceCount()).isEqualTo(2L);
        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
    }

    @Test
    void deve_lancar_404_ao_obter_notebook_inexistente_ou_de_outro_usuario() {
        // Given
        UUID notebookId = UUID.randomUUID();
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sut.getNotebook(currentUser, notebookId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notebook não encontrado");
        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
        verifyNoInteractions(sourceRepository);
    }

    @Test
    void deve_atualizar_notebook_com_sucesso() {
        // Given
        UUID notebookId = UUID.randomUUID();
        UpdateNotebookRequest request = new UpdateNotebookRequest("Nome Atualizado", "Desc Atualizada");
        Notebook notebook = new Notebook(currentUser, "Nome Antigo", "Desc Antiga");
        notebook.setId(notebookId);
        notebook.setCreatedAt(Instant.now());
        notebook.setUpdatedAt(Instant.now());

        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.of(notebook));
        when(notebookRepository.save(notebook)).thenReturn(notebook);
        when(sourceRepository.countByNotebookIdAndStatus(notebookId, SourceStatus.READY)).thenReturn(1L);

        // When
        NotebookResponse result = sut.updateNotebook(currentUser, notebookId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Nome Atualizado");
        assertThat(result.description()).isEqualTo("Desc Atualizada");
        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
        verify(notebookRepository).save(notebook);
    }

    @Test
    void deve_lancar_404_ao_atualizar_notebook_inexistente_ou_de_outro_usuario() {
        // Given
        UUID notebookId = UUID.randomUUID();
        UpdateNotebookRequest request = new UpdateNotebookRequest("Nome Atualizado", "Desc Atualizada");
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sut.updateNotebook(currentUser, notebookId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notebook não encontrado");
        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
        verify(notebookRepository, never()).save(any());
    }

    @Test
    void deve_excluir_notebook_com_sucesso() {
        // Given
        UUID notebookId = UUID.randomUUID();
        Notebook notebook = new Notebook(currentUser, "Meu Notebook", "Desc");
        notebook.setId(notebookId);

        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.of(notebook));

        // When
        sut.deleteNotebook(currentUser, notebookId);

        // Then
        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
        verify(notebookRepository).delete(notebook);
    }

    @Test
    void deve_lancar_404_ao_excluir_notebook_inexistente_ou_de_outro_usuario() {
        // Given
        UUID notebookId = UUID.randomUUID();
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sut.deleteNotebook(currentUser, notebookId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notebook não encontrado");
        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
        verify(notebookRepository, never()).delete(any());
    }
}
