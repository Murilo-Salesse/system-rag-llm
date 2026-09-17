package github.salessew.notebooklm.domain.entity;

import com.pgvector.PGvector;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SourceChunkTest {

    @Test
    void deve_instanciar_e_acessar_atributos_corretamente() {
        Source source = new Source();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        PGvector vector = new PGvector(new float[]{0.1f, 0.2f, 0.3f});

        SourceChunk chunk = new SourceChunk(source, "Sample text content", vector, 0);
        chunk.setId(id);
        chunk.setCreatedAt(now);

        assertThat(chunk.getId()).isEqualTo(id);
        assertThat(chunk.getSource()).isEqualTo(source);
        assertThat(chunk.getContent()).isEqualTo("Sample text content");
        assertThat(chunk.getEmbedding()).isEqualTo(vector);
        assertThat(chunk.getChunkIndex()).isEqualTo(0);
        assertThat(chunk.getCreatedAt()).isEqualTo(now);

        Source newSource = new Source();
        PGvector newVector = new PGvector(new float[]{0.4f, 0.5f, 0.6f});
        chunk.setSource(newSource);
        chunk.setContent("Updated text");
        chunk.setEmbedding(newVector);
        chunk.setChunkIndex(1);

        assertThat(chunk.getSource()).isEqualTo(newSource);
        assertThat(chunk.getContent()).isEqualTo("Updated text");
        assertThat(chunk.getEmbedding()).isEqualTo(newVector);
        assertThat(chunk.getChunkIndex()).isEqualTo(1);
    }

    @Test
    void deve_testar_equals_e_hashcode_baseado_em_id() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        SourceChunk c1 = new SourceChunk();
        c1.setId(id1);

        SourceChunk c2 = new SourceChunk();
        c2.setId(id1);

        SourceChunk c3 = new SourceChunk();
        c3.setId(id2);

        assertThat(c1).isEqualTo(c1);
        assertThat(c1).isEqualTo(c2);
        assertThat(c1).hasSameHashCodeAs(c2);

        assertThat(c1).isNotEqualTo(c3);
        assertThat(c1).isNotEqualTo(null);
        assertThat(c1).isNotEqualTo("some object");

        SourceChunk empty1 = new SourceChunk();
        SourceChunk empty2 = new SourceChunk();
        assertThat(empty1).isNotEqualTo(empty2);

        assertThat(c1.hashCode()).isNotZero();
    }
}
