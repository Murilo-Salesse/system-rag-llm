package github.salessew.notebooklm.domain.entity;

import github.salessew.notebooklm.domain.enums.SourceStatus;
import github.salessew.notebooklm.domain.enums.SourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTest {

    @Test
    void deve_instanciar_e_acessar_atributos_corretamente() {
        Notebook notebook = new Notebook();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        Source source = new Source(notebook, "doc.md", SourceType.MARKDOWN, "s3/key/doc.md", null);
        source.setId(id);
        source.setCreatedAt(now);

        assertThat(source.getId()).isEqualTo(id);
        assertThat(source.getNotebook()).isEqualTo(notebook);
        assertThat(source.getName()).isEqualTo("doc.md");
        assertThat(source.getType()).isEqualTo(SourceType.MARKDOWN);
        assertThat(source.getS3Key()).isEqualTo("s3/key/doc.md");
        assertThat(source.getUrl()).isNull();
        assertThat(source.getStatus()).isEqualTo(SourceStatus.PENDING);
        assertThat(source.getErrorMessage()).isNull();
        assertThat(source.getCreatedAt()).isEqualTo(now);

        Notebook newNotebook = new Notebook();
        source.setNotebook(newNotebook);
        source.setName("new.docx");
        source.setType(SourceType.DOCX);
        source.setS3Key("s3/key/new.docx");
        source.setUrl("http://example.com");
        source.setStatus(SourceStatus.PROCESSING);
        source.setErrorMessage("err");

        assertThat(source.getNotebook()).isEqualTo(newNotebook);
        assertThat(source.getName()).isEqualTo("new.docx");
        assertThat(source.getType()).isEqualTo(SourceType.DOCX);
        assertThat(source.getS3Key()).isEqualTo("s3/key/new.docx");
        assertThat(source.getUrl()).isEqualTo("http://example.com");
        assertThat(source.getStatus()).isEqualTo(SourceStatus.PROCESSING);
        assertThat(source.getErrorMessage()).isEqualTo("err");
    }

    @Test
    void deve_realizar_transicoes_de_status_adequadamente() {
        Source source = new Source(new Notebook(), "page", SourceType.WEB_URL, null, "https://example.com");
        assertThat(source.getStatus()).isEqualTo(SourceStatus.PENDING);

        source.markProcessing();
        assertThat(source.getStatus()).isEqualTo(SourceStatus.PROCESSING);
        assertThat(source.getErrorMessage()).isNull();

        source.markFailed("Scraping failed with HTTP 500");
        assertThat(source.getStatus()).isEqualTo(SourceStatus.FAILED);
        assertThat(source.getErrorMessage()).isEqualTo("Scraping failed with HTTP 500");

        source.markReady();
        assertThat(source.getStatus()).isEqualTo(SourceStatus.READY);
        assertThat(source.getErrorMessage()).isNull();
    }

    @Test
    void deve_testar_equals_e_hashcode_baseado_em_id() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Source s1 = new Source();
        s1.setId(id1);

        Source s2 = new Source();
        s2.setId(id1);

        Source s3 = new Source();
        s3.setId(id2);

        assertThat(s1).isEqualTo(s1);
        assertThat(s1).isEqualTo(s2);
        assertThat(s1).hasSameHashCodeAs(s2);

        assertThat(s1).isNotEqualTo(s3);
        assertThat(s1).isNotEqualTo(null);
        assertThat(s1).isNotEqualTo("other object");

        Source empty1 = new Source();
        Source empty2 = new Source();
        assertThat(empty1).isNotEqualTo(empty2);

        assertThat(s1.hashCode()).isNotZero();
    }
}
