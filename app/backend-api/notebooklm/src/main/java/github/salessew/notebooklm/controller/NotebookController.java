package github.salessew.notebooklm.controller;

import github.salessew.notebooklm.domain.entity.User;
import github.salessew.notebooklm.dto.CreateNotebookRequest;
import github.salessew.notebooklm.dto.NotebookResponse;
import github.salessew.notebooklm.dto.UpdateNotebookRequest;
import github.salessew.notebooklm.service.NotebookService;
import github.salessew.notebooklm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notebooks")
public class NotebookController {

    private final NotebookService notebookService;
    private final UserService userService;

    public NotebookController(NotebookService notebookService, UserService userService) {
        this.notebookService = notebookService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Page<NotebookResponse>> listNotebooks(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User currentUser = userService.getOrCreateCurrentUser(jwt);
        Page<NotebookResponse> notebooks = notebookService.listNotebooks(currentUser, pageable);
        return ResponseEntity.ok(notebooks);
    }

    @PostMapping
    public ResponseEntity<NotebookResponse> createNotebook(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateNotebookRequest request
    ) {
        User currentUser = userService.getOrCreateCurrentUser(jwt);
        NotebookResponse response = notebookService.createNotebook(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{notebookId}")
    public ResponseEntity<NotebookResponse> getNotebook(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID notebookId
    ) {
        User currentUser = userService.getOrCreateCurrentUser(jwt);
        NotebookResponse response = notebookService.getNotebook(currentUser, notebookId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{notebookId}")
    public ResponseEntity<NotebookResponse> updateNotebook(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID notebookId,
            @Valid @RequestBody UpdateNotebookRequest request
    ) {
        User currentUser = userService.getOrCreateCurrentUser(jwt);
        NotebookResponse response = notebookService.updateNotebook(currentUser, notebookId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{notebookId}")
    public ResponseEntity<Void> deleteNotebook(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID notebookId
    ) {
        User currentUser = userService.getOrCreateCurrentUser(jwt);
        notebookService.deleteNotebook(currentUser, notebookId);
        return ResponseEntity.noContent().build();
    }
}
