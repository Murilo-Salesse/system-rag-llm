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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class SourceService {

    private static final long MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024; // 25MB

    private final SourceRepository sourceRepository;
    private final NotebookRepository notebookRepository;
    private final DocumentParserService documentParserService;
    private final SourceIngestionProcessor ingestionProcessor;

    public SourceService(
            SourceRepository sourceRepository,
            NotebookRepository notebookRepository,
            DocumentParserService documentParserService,
            SourceIngestionProcessor ingestionProcessor
    ) {
        this.sourceRepository = sourceRepository;
        this.notebookRepository = notebookRepository;
        this.documentParserService = documentParserService;
        this.ingestionProcessor = ingestionProcessor;
    }

    @Transactional
    public SourceUploadResponse initiateFileUpload(User currentUser, UUID notebookId, MultipartFile file, String customName) {
        Notebook notebook = findOwnedNotebookOrThrow(currentUser, notebookId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio ou não fornecido.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Tamanho do arquivo excede o limite máximo permitido de 25MB.");
        }

        String originalFilename = file.getOriginalFilename();
        SourceType type = documentParserService.determineSourceType(originalFilename);

        String sourceName = (customName != null && !customName.isBlank()) ? customName.trim() : originalFilename;

        Source source = new Source(notebook, sourceName, type, null, null);
        source.setStatus(SourceStatus.PENDING);
        Source savedSource = sourceRepository.save(source);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Falha ao ler bytes do arquivo: " + e.getMessage(), e);
        }

        // Disparo do pipeline assíncrono em background
        ingestionProcessor.processSourceFile(
                savedSource.getId(),
                fileBytes,
                originalFilename,
                file.getContentType()
        );

        String statusUrl = String.format("/api/v1/notebooks/%s/sources/%s", notebookId, savedSource.getId());

        return new SourceUploadResponse(
                savedSource.getId(),
                notebookId,
                savedSource.getName(),
                savedSource.getType().name(),
                savedSource.getStatus().name(),
                "Upload aceito com sucesso. Processamento assíncrono em andamento.",
                statusUrl
        );
    }

    @Transactional(readOnly = true)
    public SourceStatusResponse getSourceStatus(User currentUser, UUID notebookId, UUID sourceId) {
        findOwnedNotebookOrThrow(currentUser, notebookId);

        Source source = sourceRepository.findByIdAndNotebookId(sourceId, notebookId)
                .orElseThrow(() -> new ResourceNotFoundException("Fonte não encontrada no notebook."));

        return new SourceStatusResponse(
                source.getId(),
                notebookId,
                source.getName(),
                source.getType().name(),
                source.getStatus().name(),
                source.getErrorMessage(),
                source.getCreatedAt()
        );
    }

    private Notebook findOwnedNotebookOrThrow(User currentUser, UUID notebookId) {
        return notebookRepository.findByIdAndOwnerId(notebookId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notebook não encontrado"));
    }
}
