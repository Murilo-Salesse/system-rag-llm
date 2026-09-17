package github.salessew.notebooklm.domain.entity;

import com.pgvector.PGvector;
import github.salessew.notebooklm.domain.enums.MessageRole;
import github.salessew.notebooklm.domain.enums.SourceStatus;
import github.salessew.notebooklm.domain.enums.SourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntitiesTest {

    @Test
    void testUserEntity() {
        User user = new User("sub-1", "test@user.com", "Test User");
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        user.setId(id);
        user.setCognitoSub("sub-2");
        user.setEmail("test2@user.com");
        user.setName("New Name");
        user.setCreatedAt(now);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getCognitoSub()).isEqualTo("sub-2");
        assertThat(user.getEmail()).isEqualTo("test2@user.com");
        assertThat(user.getName()).isEqualTo("New Name");
        assertThat(user.getCreatedAt()).isEqualTo(now);

        User sameUser = new User();
        sameUser.setId(id);
        sameUser.setCognitoSub("sub-2");
        User differentUser = new User();
        differentUser.setId(UUID.randomUUID());
        differentUser.setCognitoSub("sub-3");

        assertThat(user).isEqualTo(user);
        assertThat(user).isEqualTo(sameUser);
        assertThat(user).isNotEqualTo(differentUser);
        assertThat(user).isNotEqualTo(null);
        assertThat(user).isNotEqualTo(new Object());
        assertThat(user.hashCode()).isEqualTo(sameUser.hashCode());
    }

    @Test
    void testNotebookEntity() {
        User user = new User("sub-1", "user@test.com", "User");
        Notebook notebook = new Notebook(user, "Title", "Desc");
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        notebook.setId(id);
        notebook.setOwner(user);
        notebook.setName("New Name");
        notebook.setDescription("New Desc");
        notebook.setCreatedAt(now);
        notebook.setUpdatedAt(now);

        assertThat(notebook.getId()).isEqualTo(id);
        assertThat(notebook.getOwner()).isEqualTo(user);
        assertThat(notebook.getName()).isEqualTo("New Name");
        assertThat(notebook.getDescription()).isEqualTo("New Desc");
        assertThat(notebook.getCreatedAt()).isEqualTo(now);
        assertThat(notebook.getUpdatedAt()).isEqualTo(now);

        Notebook sameNotebook = new Notebook();
        sameNotebook.setId(id);
        Notebook diff = new Notebook();
        diff.setId(UUID.randomUUID());

        assertThat(notebook).isEqualTo(notebook);
        assertThat(notebook).isEqualTo(sameNotebook);
        assertThat(notebook).isNotEqualTo(diff);
        assertThat(notebook).isNotEqualTo(null);
        assertThat(notebook).isNotEqualTo(new Object());
        assertThat(notebook.hashCode()).isEqualTo(sameNotebook.hashCode());
    }

    @Test
    void testConversationEntity() {
        User user = new User("sub-1", "user@test.com", "User");
        Notebook notebook = new Notebook(user, "Title", "Desc");
        Conversation conv = new Conversation(notebook);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        conv.setId(id);
        conv.setNotebook(notebook);
        conv.setCreatedAt(now);
        conv.setActiveSources(null);
        assertThat(conv.getActiveSources()).isEmpty();

        Source source = new Source(notebook, "doc.md", SourceType.MARKDOWN, null, null);
        source.setId(UUID.randomUUID());
        conv.addActiveSource(source);
        assertThat(conv.getActiveSources()).contains(source);
        conv.removeActiveSource(source);
        assertThat(conv.getActiveSources()).doesNotContain(source);

        conv.setActiveSources(new HashSet<>(Set.of(source)));
        assertThat(conv.getActiveSources()).contains(source);

        assertThat(conv.getId()).isEqualTo(id);
        assertThat(conv.getNotebook()).isEqualTo(notebook);
        assertThat(conv.getCreatedAt()).isEqualTo(now);

        Conversation sameConv = new Conversation();
        sameConv.setId(id);
        Conversation diff = new Conversation();
        diff.setId(UUID.randomUUID());

        assertThat(conv).isEqualTo(conv);
        assertThat(conv).isEqualTo(sameConv);
        assertThat(conv).isNotEqualTo(diff);
        assertThat(conv).isNotEqualTo(null);
        assertThat(conv).isNotEqualTo(new Object());
        assertThat(conv.hashCode()).isEqualTo(sameConv.hashCode());
    }

    @Test
    void testConversationMessageEntity() {
        User user = new User("sub-1", "user@test.com", "User");
        Notebook notebook = new Notebook(user, "Title", "Desc");
        Conversation conv = new Conversation(notebook);
        ConversationMessage msg = new ConversationMessage(conv, MessageRole.USER, "Ola");
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        msg.setId(id);
        msg.setConversation(conv);
        msg.setRole(MessageRole.ASSISTANT);
        msg.setContent("Resposta");
        msg.setCreatedAt(now);

        assertThat(msg.getId()).isEqualTo(id);
        assertThat(msg.getConversation()).isEqualTo(conv);
        assertThat(msg.getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(msg.getContent()).isEqualTo("Resposta");
        assertThat(msg.getCreatedAt()).isEqualTo(now);

        ConversationMessage sameMsg = new ConversationMessage();
        sameMsg.setId(id);
        ConversationMessage diff = new ConversationMessage();
        diff.setId(UUID.randomUUID());

        assertThat(msg).isEqualTo(msg);
        assertThat(msg).isEqualTo(sameMsg);
        assertThat(msg).isNotEqualTo(diff);
        assertThat(msg).isNotEqualTo(null);
        assertThat(msg).isNotEqualTo(new Object());
        assertThat(msg.hashCode()).isEqualTo(sameMsg.hashCode());
    }

    @Test
    void testSourceAndSourceChunkEntity() {
        User user = new User("sub-1", "user@test.com", "User");
        Notebook notebook = new Notebook(user, "Title", "Desc");
        Source source = new Source(notebook, "doc.md", SourceType.MARKDOWN, "s3/key", "http://example.com");
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        source.setId(id);
        source.setNotebook(notebook);
        source.setName("new.md");
        source.setType(SourceType.DOCX);
        source.setS3Key("new/s3");
        source.setUrl("http://new.com");
        source.setStatus(SourceStatus.READY);
        source.setErrorMessage("err");
        source.setCreatedAt(now);

        assertThat(source.getId()).isEqualTo(id);
        assertThat(source.getNotebook()).isEqualTo(notebook);
        assertThat(source.getName()).isEqualTo("new.md");
        assertThat(source.getType()).isEqualTo(SourceType.DOCX);
        assertThat(source.getS3Key()).isEqualTo("new/s3");
        assertThat(source.getUrl()).isEqualTo("http://new.com");
        assertThat(source.getStatus()).isEqualTo(SourceStatus.READY);
        assertThat(source.getErrorMessage()).isEqualTo("err");
        assertThat(source.getCreatedAt()).isEqualTo(now);

        Source sameSource = new Source();
        sameSource.setId(id);
        Source diffSource = new Source();
        diffSource.setId(UUID.randomUUID());

        assertThat(source).isEqualTo(source);
        assertThat(source).isEqualTo(sameSource);
        assertThat(source).isNotEqualTo(diffSource);
        assertThat(source).isNotEqualTo(null);
        assertThat(source).isNotEqualTo(new Object());
        assertThat(source.hashCode()).isEqualTo(sameSource.hashCode());

        // SourceChunk
        PGvector vec1 = new PGvector(new float[]{0.1f, 0.2f});
        SourceChunk chunk = new SourceChunk(source, "content", vec1, 1);
        UUID chunkId = UUID.randomUUID();
        chunk.setId(chunkId);
        chunk.setSource(source);
        chunk.setContent("new content");
        PGvector vec2 = new PGvector(new float[]{0.3f});
        chunk.setEmbedding(vec2);
        chunk.setChunkIndex(2);
        chunk.setCreatedAt(now);

        assertThat(chunk.getId()).isEqualTo(chunkId);
        assertThat(chunk.getSource()).isEqualTo(source);
        assertThat(chunk.getContent()).isEqualTo("new content");
        assertThat(chunk.getEmbedding()).isEqualTo(vec2);
        assertThat(chunk.getChunkIndex()).isEqualTo(2);
        assertThat(chunk.getCreatedAt()).isEqualTo(now);

        SourceChunk sameChunk = new SourceChunk();
        sameChunk.setId(chunkId);
        SourceChunk diffChunk = new SourceChunk();
        diffChunk.setId(UUID.randomUUID());

        assertThat(chunk).isEqualTo(chunk);
        assertThat(chunk).isEqualTo(sameChunk);
        assertThat(chunk).isNotEqualTo(diffChunk);
        assertThat(chunk).isNotEqualTo(null);
        assertThat(chunk).isNotEqualTo(new Object());
        assertThat(chunk.hashCode()).isEqualTo(sameChunk.hashCode());
    }
}
