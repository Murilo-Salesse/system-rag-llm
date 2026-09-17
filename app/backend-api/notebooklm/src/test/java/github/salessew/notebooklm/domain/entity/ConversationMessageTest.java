package github.salessew.notebooklm.domain.entity;

import github.salessew.notebooklm.domain.enums.MessageRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationMessageTest {

    @Test
    void deve_instanciar_e_acessar_atributos_corretamente() {
        Conversation conv = new Conversation();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        ConversationMessage msg = new ConversationMessage(conv, MessageRole.USER, "Hello AI");
        msg.setId(id);
        msg.setCreatedAt(now);

        assertThat(msg.getId()).isEqualTo(id);
        assertThat(msg.getConversation()).isEqualTo(conv);
        assertThat(msg.getRole()).isEqualTo(MessageRole.USER);
        assertThat(msg.getContent()).isEqualTo("Hello AI");
        assertThat(msg.getCreatedAt()).isEqualTo(now);

        Conversation newConv = new Conversation();
        msg.setConversation(newConv);
        msg.setRole(MessageRole.ASSISTANT);
        msg.setContent("Hello User");

        assertThat(msg.getConversation()).isEqualTo(newConv);
        assertThat(msg.getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(msg.getContent()).isEqualTo("Hello User");
    }

    @Test
    void deve_testar_equals_e_hashcode_baseado_em_id() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        ConversationMessage m1 = new ConversationMessage();
        m1.setId(id1);

        ConversationMessage m2 = new ConversationMessage();
        m2.setId(id1);

        ConversationMessage m3 = new ConversationMessage();
        m3.setId(id2);

        assertThat(m1).isEqualTo(m1);
        assertThat(m1).isEqualTo(m2);
        assertThat(m1).hasSameHashCodeAs(m2);

        assertThat(m1).isNotEqualTo(m3);
        assertThat(m1).isNotEqualTo(null);
        assertThat(m1).isNotEqualTo("another object");

        ConversationMessage empty1 = new ConversationMessage();
        ConversationMessage empty2 = new ConversationMessage();
        assertThat(empty1).isNotEqualTo(empty2);

        assertThat(m1.hashCode()).isNotZero();
    }
}
