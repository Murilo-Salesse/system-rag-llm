package github.salessew.notebooklm.service;

import github.salessew.notebooklm.domain.entity.Notebook;
import github.salessew.notebooklm.domain.entity.Source;
import github.salessew.notebooklm.domain.entity.User;
import github.salessew.notebooklm.domain.enums.SourceStatus;
import github.salessew.notebooklm.domain.enums.SourceType;
import github.salessew.notebooklm.domain.repository.SourceChunkRepository;
import github.salessew.notebooklm.domain.repository.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SourceIngestionProcessorTest {

    private S3StorageService s3StorageService;
    private DocumentParserService documentParserService;
    private SourceRepository sourceRepository;
    private SourceChunkRepository sourceChunkRepository;
    private EmbeddingModel embeddingModel;
    private SourceIngestionProcessor sut;

    private Source source;
    private UUID sourceId;
    private UUID notebookId;

    @BeforeEach
    void setUp() {
        s3StorageService = mock(S3StorageService.class);
        documentParserService = mock(DocumentParserService.class);
        sourceRepository = mock(SourceRepository.class);
        sourceChunkRepository = mock(SourceChunkRepository.class);
        embeddingModel = mock(EmbeddingModel.class);

        sut = new SourceIngestionProcessor(
                s3StorageService,
                documentParserService,
                sourceRepository,
                sourceChunkRepository,
                embeddingModel
        );

        User user = new User("sub-1", "user@test.com", "User");
        notebookId = UUID.randomUUID();
        Notebook notebook = new Notebook(user, "Notebook", "Desc");
        notebook.setId(notebookId);

        sourceId = UUID.randomUUID();
        source = new Source(notebook, "doc.md", SourceType.MARKDOWN, null, null);
        source.setId(sourceId);
    }

    @Test
    void deve_processar_arquivo_com_sucesso() {
        // Given
        byte[] bytes = "# Titulo\nTexto de teste.".getBytes();
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(s3StorageService.uploadFile(eq(notebookId), eq(sourceId), eq("doc.md"), eq(bytes), eq("text/markdown")))
                .thenReturn("notebooks/nb/sources/src/doc.md");
        when(documentParserService.parseDocument(bytes, SourceType.MARKDOWN))
                .thenReturn("Texto de teste extraido.");
        when(embeddingModel.embed(any(String.class)))
                .thenReturn(new float[]{0.1f, 0.2f});

        // When
        sut.processSourceFile(sourceId, bytes, "doc.md", "text/markdown");

        // Then
        assertThat(source.getStatus()).isEqualTo(SourceStatus.READY);
        assertThat(source.getS3Key()).isEqualTo("notebooks/nb/sources/src/doc.md");
        assertThat(source.getErrorMessage()).isNull();

        verify(sourceRepository, atLeast(2)).save(source);
        verify(sourceChunkRepository).saveAll(anyList());
    }

    @Test
    void deve_marcar_como_failed_quando_ocorrer_erro() {
        // Given
        byte[] bytes = "corrupt".getBytes();
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(s3StorageService.uploadFile(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("S3 indisponível"));

        // When
        sut.processSourceFile(sourceId, bytes, "doc.md", "text/markdown");

        // Then
        assertThat(source.getStatus()).isEqualTo(SourceStatus.FAILED);
        assertThat(source.getErrorMessage()).contains("S3 indisponível");

        verify(sourceRepository, atLeast(2)).save(source);
        verifyNoInteractions(sourceChunkRepository);
    }

    @Test
    void nao_deve_processar_quando_fonte_nao_encontrada() {
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.empty());

        sut.processSourceFile(sourceId, new byte[0], "doc.md", "text/markdown");

        verify(sourceRepository).findById(sourceId);
        verifyNoMoreInteractions(sourceRepository);
        verifyNoInteractions(s3StorageService);
    }
}
