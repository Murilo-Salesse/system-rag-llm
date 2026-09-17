package github.salessew.notebooklm.domain.repository;

import github.salessew.notebooklm.domain.entity.Notebook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotebookRepository extends JpaRepository<Notebook, UUID> {

    List<Notebook> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Page<Notebook> findByOwnerId(UUID ownerId, Pageable pageable);

    Optional<Notebook> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}
