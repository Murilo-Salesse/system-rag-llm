package github.salessew.notebooklm.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationTest {

    @Test
    void deve_instanciar_e_acessar_atributos_corretamente() {
        Notebook notebook = new Notebook();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        Conversation conv = new Conversation(notebook);
        conv.setId(id);
        conv.setCreatedAt(now);

        assertThat(conv.getId()).isEqualTo(id);
        assertThat(conv.getNotebook()).isEqualTo(notebook);
        assertThat(conv.getCreatedAt()).isEqualTo(now);
        assertThat(conv.getActiveSources()).isNotNull().isEmpty();

        Notebook newNotebook = new Notebook();
        conv.setNotebook(newNotebook);
        assertThat(conv.getNotebook()).isEqualTo(newNotebook);
    }

    @Test
    void deve_manipular_fontes_ativas_adequadamente() {
        Conversation conv = new Conversation();
        Source s1 = new Source();
        s1.setId(UUID.randomUUID());
        Source s2 = new Source();
        s2.setId(UUID.randomUUID());

        conv.addActiveSource(s1);
        conv.addActiveSource(s2);
        assertThat(conv.getActiveSources()).containsExactlyInAnyOrder(s1, s2);

        conv.removeActiveSource(s1);
        assertThat(conv.getActiveSources()).containsExactly(s2);

        conv.setActiveSources(null);
        assertThat(conv.getActiveSources()).isEmpty();

        Set<Source> newSet = new HashSet<>();
        newSet.add(s1);
        conv.setActiveSources(newSet);
        assertThat(conv.getActiveSources()).containsExactly(s1);
    }

    @Test
    void deve_testar_equals_e_hashcode_baseado_em_id() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Conversation c1 = new Conversation();
        c1.setId(id1);

        Conversation c2 = new Conversation();
        c2.setId(id1);

        Conversation c3 = new Conversation();
        c3.setId(id2);

        assertThat(c1).isEqualTo(c1);
        assertThat(c1).isEqualTo(c2);
        assertThat(c1).hasSameHashCodeAs(c2);

        assertThat(c1).isNotEqualTo(c3);
        assertThat(c1).isNotEqualTo(null);
        assertThat(c1).isNotEqualTo("other object");

        Conversation empty1 = new Conversation();
        Conversation empty2 = new Conversation();
        assertThat(empty1).isNotEqualTo(empty2);

        assertThat(c1.hashCode()).isNotZero();
    }
}
