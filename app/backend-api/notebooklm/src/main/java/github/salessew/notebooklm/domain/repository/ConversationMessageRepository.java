package github.salessew.notebooklm.domain.repository;

import github.salessew.notebooklm.domain.entity.ConversationMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    Page<ConversationMessage> findByConversationId(UUID conversationId, Pageable pageable);

    long countByConversationId(UUID conversationId);

    void deleteByConversationId(UUID conversationId);
}
