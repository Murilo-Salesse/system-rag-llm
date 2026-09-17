package github.salessew.notebooklm.domain.repository;

import github.salessew.notebooklm.domain.entity.SourceChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SourceChunkRepository extends JpaRepository<SourceChunk, UUID> {

    List<SourceChunk> findBySourceIdOrderByChunkIndexAsc(UUID sourceId);

    long countBySourceId(UUID sourceId);
}
