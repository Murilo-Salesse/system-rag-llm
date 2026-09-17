package github.salessew.notebooklm.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

class AwsS3ConfigTest {

    @Test
    void deve_criar_s3_client_com_endpoint_override() {
        AwsS3Config config = new AwsS3Config();
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "endpoint", "http://localhost:4566");

        try (S3Client client = config.s3Client()) {
            assertThat(client).isNotNull();
            assertThat(client.serviceClientConfiguration().endpointOverride()).isPresent();
            assertThat(client.serviceClientConfiguration().endpointOverride().get().toString())
                    .isEqualTo("http://localhost:4566");
        }
    }

    @Test
    void deve_criar_s3_client_sem_endpoint_override() {
        AwsS3Config config = new AwsS3Config();
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "endpoint", "");

        try (S3Client client = config.s3Client()) {
            assertThat(client).isNotNull();
            assertThat(client.serviceClientConfiguration().endpointOverride()).isEmpty();
        }
    }
}
