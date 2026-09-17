package github.salessew.notebooklm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3StorageServiceTest {

    private S3Client s3Client;
    private S3StorageService sut;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        sut = new S3StorageService(s3Client, "test-bucket");
    }

    @Test
    void deve_fazer_upload_de_arquivo_com_sucesso() {
        UUID notebookId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        String filename = "documento.md";
        byte[] content = "# Titulo".getBytes();

        String resultKey = sut.uploadFile(notebookId, sourceId, filename, content, "text/markdown");

        String expectedKey = String.format("notebooks/%s/sources/%s/%s", notebookId, sourceId, filename);
        assertThat(resultKey).isEqualTo(expectedKey);
        assertThat(sut.getBucketName()).isEqualTo("test-bucket");

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
