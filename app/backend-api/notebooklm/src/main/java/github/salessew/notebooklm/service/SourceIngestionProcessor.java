package github.salessew.notebooklm.service;

import com.pgvector.PGvector;
import github.salessew.notebooklm.domain.entity.Source;
import github.salessew.notebooklm.domain.entity.SourceChunk;
import github.salessew.notebooklm.domain.enums.SourceStatus;
import github.salessew.notebooklm.domain.enums.SourceType;
import github.salessew.notebooklm.domain.repository.SourceChunkRepository;
import github.salessew.notebooklm.domain.repository.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SourceIngestionProcessor {

    private static final Logger log = LoggerFactory.getLogger(SourceIngestionProcessor.class);

    private final S3StorageService s3StorageService;
    private final DocumentParserService documentParserService;
    private final SourceRepository sourceRepository;
    private final SourceChunkRepository sourceChunkRepository;
    private final EmbeddingModel embeddingModel;

    public SourceIngestionProcessor(
            S3StorageService s3StorageService,
            DocumentParserService documentParserService,
            SourceRepository sourceRepository,
            SourceChunkRepository sourceChunkRepository,
            EmbeddingModel embeddingModel
    ) {
        this.s3StorageService = s3StorageService;
        this.documentParserService = documentParserService;
        this.sourceRepository = sourceRepository;
        this.sourceChunkRepository = sourceChunkRepository;
        this.embeddingModel = embeddingModel;
    }

    @Async
    @Transactional
    public void processSourceFile(UUID sourceId, byte[] fileBytes, String originalFilename, String contentType) {
        Source source = sourceRepository.findById(sourceId).orElse(null);
        if (source == null) {
            log.warn("Fonte {} não encontrada para processamento.", sourceId);
            return;
        }

        try {
            source.setStatus(SourceStatus.PROCESSING);
            sourceRepository.save(source);

            // 1. Upload do arquivo original no S3
            String s3Key = s3StorageService.uploadFile(
                    source.getNotebook().getId(),
                    source.getId(),
                    originalFilename,
                    fileBytes,
                    contentType
            );
            source.setS3Key(s3Key);

            // 2. Extração de texto limpo
            SourceType type = source.getType();
            String fullText = documentParserService.parseDocument(fileBytes, type);

            // 3. Chunking de texto
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> documents = splitter.split(List.of(new Document(fullText)));

            // 4. Geração de Embeddings e persistência de chunks
            List<SourceChunk> chunksToSave = new ArrayList<>();
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                float[] embedding = embeddingModel.embed(doc.getText());

                SourceChunk chunk = new SourceChunk(
                        source,
                        doc.getText(),
                        new PGvector(embedding),
                        i
                );
                chunksToSave.add(chunk);
            }

            sourceChunkRepository.saveAll(chunksToSave);

            source.setStatus(SourceStatus.READY);
            source.setErrorMessage(null);
            sourceRepository.save(source);

            log.info("Processamento da fonte {} concluído com sucesso. Chunks: {}", sourceId, chunksToSave.size());

        } catch (Exception e) {
            log.error("Erro no processamento da fonte {}: {}", sourceId, e.getMessage(), e);
            source.setStatus(SourceStatus.FAILED);
            source.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Erro desconhecido durante o processamento.");
            sourceRepository.save(source);
        }
    }
}
