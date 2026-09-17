package github.salessew.notebooklm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final String bucketName;

    public S3StorageService(
            S3Client s3Client,
            @Value("${aws.s3.bucket:notebooklm-sources}") String bucketName
    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public String uploadFile(UUID notebookId, UUID sourceId, String originalFilename, byte[] content, String contentType) {
        String key = String.format("notebooks/%s/sources/%s/%s", notebookId, sourceId, originalFilename);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(content));
        return key;
    }

    public String getBucketName() {
        return bucketName;
    }
}
