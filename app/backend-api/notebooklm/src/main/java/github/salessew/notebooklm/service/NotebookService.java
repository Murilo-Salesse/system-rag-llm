package github.salessew.notebooklm.service;

import github.salessew.notebooklm.domain.entity.Notebook;
import github.salessew.notebooklm.domain.entity.User;
import github.salessew.notebooklm.domain.enums.SourceStatus;
import github.salessew.notebooklm.domain.repository.NotebookRepository;
import github.salessew.notebooklm.domain.repository.SourceRepository;
import github.salessew.notebooklm.dto.CreateNotebookRequest;
import github.salessew.notebooklm.dto.NotebookResponse;
import github.salessew.notebooklm.dto.UpdateNotebookRequest;
import github.salessew.notebooklm.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotebookService {

    private final NotebookRepository notebookRepository;
    private final SourceRepository sourceRepository;

    public NotebookService(NotebookRepository notebookRepository, SourceRepository sourceRepository) {
        this.notebookRepository = notebookRepository;
        this.sourceRepository = sourceRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotebookResponse> listNotebooks(User currentUser, Pageable pageable) {
        return notebookRepository.findByOwnerId(currentUser.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public NotebookResponse createNotebook(User currentUser, CreateNotebookRequest request) {
        Notebook notebook = new Notebook(currentUser, request.name(), request.description());
        Notebook saved = notebookRepository.save(notebook);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public NotebookResponse getNotebook(User currentUser, UUID notebookId) {
        Notebook notebook = notebookRepository.findByIdAndOwnerId(notebookId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notebook não encontrado"));
        return toResponse(notebook);
    }

    @Transactional
    public NotebookResponse updateNotebook(User currentUser, UUID notebookId, UpdateNotebookRequest request) {
        Notebook notebook = notebookRepository.findByIdAndOwnerId(notebookId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notebook não encontrado"));

        notebook.setName(request.name());
        notebook.setDescription(request.description());
        Notebook updated = notebookRepository.save(notebook);
        return toResponse(updated);
    }

    @Transactional
    public void deleteNotebook(User currentUser, UUID notebookId) {
        Notebook notebook = notebookRepository.findByIdAndOwnerId(notebookId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notebook não encontrado"));

        notebookRepository.delete(notebook);
    }

    private NotebookResponse toResponse(Notebook notebook) {
        long sourceCount = sourceRepository.countByNotebookIdAndStatus(notebook.getId(), SourceStatus.READY);
        return new NotebookResponse(
                notebook.getId(),
                notebook.getOwner().getId(),
                notebook.getName(),
                notebook.getDescription(),
                sourceCount,
                notebook.getCreatedAt(),
                notebook.getUpdatedAt()
        );
    }
}
