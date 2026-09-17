package github.salessew.notebooklm.domain.repository;

import github.salessew.notebooklm.domain.entity.Source;
import github.salessew.notebooklm.domain.enums.SourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourceRepository extends JpaRepository<Source, UUID> {

    List<Source> findByNotebookIdOrderByCreatedAtDesc(UUID notebookId);

    List<Source> findByNotebookIdAndStatus(UUID notebookId, SourceStatus status);

    Optional<Source> findByIdAndNotebookId(UUID id, UUID notebookId);

    long countByNotebookIdAndStatus(UUID notebookId, SourceStatus status);
}
