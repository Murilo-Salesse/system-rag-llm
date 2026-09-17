package github.salessew.notebooklm.domain.repository;

import github.salessew.notebooklm.domain.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByNotebookIdOrderByCreatedAtDesc(UUID notebookId);

    Optional<Conversation> findByIdAndNotebookId(UUID id, UUID notebookId);

    void deleteByNotebookId(UUID notebookId);
}
