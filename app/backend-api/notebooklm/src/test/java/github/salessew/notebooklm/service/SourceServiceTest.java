package github.salessew.notebooklm.service;

import github.salessew.notebooklm.domain.entity.Notebook;
import github.salessew.notebooklm.domain.entity.Source;
import github.salessew.notebooklm.domain.entity.User;
import github.salessew.notebooklm.domain.enums.SourceStatus;
import github.salessew.notebooklm.domain.enums.SourceType;
import github.salessew.notebooklm.domain.repository.NotebookRepository;
import github.salessew.notebooklm.domain.repository.SourceRepository;
import github.salessew.notebooklm.dto.SourceStatusResponse;
import github.salessew.notebooklm.dto.SourceUploadResponse;
import github.salessew.notebooklm.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SourceServiceTest {

    private SourceRepository sourceRepository;
    private NotebookRepository notebookRepository;
    private DocumentParserService documentParserService;
    private SourceIngestionProcessor ingestionProcessor;
    private SourceService sut;

    private User currentUser;
    private UUID userId;
    private UUID notebookId;
    private Notebook notebook;

    @BeforeEach
    void setUp() {
        sourceRepository = mock(SourceRepository.class);
        notebookRepository = mock(NotebookRepository.class);
        documentParserService = mock(DocumentParserService.class);
        ingestionProcessor = mock(SourceIngestionProcessor.class);

        sut = new SourceService(
                sourceRepository,
                notebookRepository,
                documentParserService,
                ingestionProcessor
        );

        userId = UUID.randomUUID();
        currentUser = new User("sub-1", "user@test.com", "User");
        currentUser.setId(userId);

        notebookId = UUID.randomUUID();
        notebook = new Notebook(currentUser, "Notebook", "Desc");
        notebook.setId(notebookId);
    }

    @Test
    void deve_iniciar_upload_de_arquivo_com_sucesso() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.md",
                "text/markdown",
                "# Conteudo".getBytes()
        );

        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.of(notebook));
        when(documentParserService.determineSourceType("manual.md")).thenReturn(SourceType.MARKDOWN);

        UUID sourceId = UUID.randomUUID();
        when(sourceRepository.save(any(Source.class))).thenAnswer(invocation -> {
            Source s = invocation.getArgument(0);
            s.setId(sourceId);
            return s;
        });

        // When
        SourceUploadResponse response = sut.initiateFileUpload(currentUser, notebookId, file, "Manual Custom");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.sourceId()).isEqualTo(sourceId);
        assertThat(response.notebookId()).isEqualTo(notebookId);
        assertThat(response.name()).isEqualTo("Manual Custom");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.type()).isEqualTo("MARKDOWN");
        assertThat(response.statusUrl()).isEqualTo("/api/v1/notebooks/" + notebookId + "/sources/" + sourceId);

        verify(sourceRepository).save(any(Source.class));
        verify(ingestionProcessor).processSourceFile(eq(sourceId), any(), eq("manual.md"), eq("text/markdown"));
    }

    @Test
    void deve_lancar_404_ao_iniciar_upload_em_notebook_de_outro_usuario() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.md", "text/markdown", "content".getBytes());
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.initiateFileUpload(currentUser, notebookId, file, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notebook não encontrado");

        verifyNoInteractions(sourceRepository);
        verifyNoInteractions(ingestionProcessor);
    }

    @Test
    void deve_lancar_400_quando_arquivo_vazio() {
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.of(notebook));
        MockMultipartFile emptyFile = new MockMultipartFile("file", "doc.md", "text/markdown", new byte[0]);

        assertThatThrownBy(() -> sut.initiateFileUpload(currentUser, notebookId, emptyFile, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Arquivo vazio ou não fornecido");

        assertThatThrownBy(() -> sut.initiateFileUpload(currentUser, notebookId, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deve_lancar_400_quando_arquivo_excede_limite_de_25mb() {
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.of(notebook));
        MockMultipartFile largeFile = mock(MockMultipartFile.class);
        when(largeFile.isEmpty()).thenReturn(false);
        when(largeFile.getSize()).thenReturn(26L * 1024 * 1024);

        assertThatThrownBy(() -> sut.initiateFileUpload(currentUser, notebookId, largeFile, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limite máximo permitido de 25MB");
    }

    @Test
    void deve_consultar_status_da_fonte_com_sucesso() {
        // Given
        UUID sourceId = UUID.randomUUID();
        Source source = new Source(notebook, "doc.md", SourceType.MARKDOWN, "s3/key", null);
        source.setId(sourceId);
        source.setStatus(SourceStatus.READY);
        source.setCreatedAt(Instant.now());

        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.of(notebook));
        when(sourceRepository.findByIdAndNotebookId(sourceId, notebookId)).thenReturn(Optional.of(source));

        // When
        SourceStatusResponse response = sut.getSourceStatus(currentUser, notebookId, sourceId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(sourceId);
        assertThat(response.notebookId()).isEqualTo(notebookId);
        assertThat(response.status()).isEqualTo("READY");
        assertThat(response.name()).isEqualTo("doc.md");
    }

    @Test
    void deve_lancar_404_ao_consultar_status_de_fonte_inexistente() {
        UUID sourceId = UUID.randomUUID();
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.of(notebook));
        when(sourceRepository.findByIdAndNotebookId(sourceId, notebookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getSourceStatus(currentUser, notebookId, sourceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Fonte não encontrada no notebook");
    }

    @Test
    void deve_lancar_404_ao_consultar_status_quando_notebook_de_outro_usuario() {
        UUID sourceId = UUID.randomUUID();
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getSourceStatus(currentUser, notebookId, sourceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notebook não encontrado");
    }
}
