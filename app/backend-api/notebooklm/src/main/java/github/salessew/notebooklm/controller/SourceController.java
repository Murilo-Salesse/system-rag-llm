package github.salessew.notebooklm.controller;

import github.salessew.notebooklm.domain.entity.User;
import github.salessew.notebooklm.dto.SourceStatusResponse;
import github.salessew.notebooklm.dto.SourceUploadResponse;
import github.salessew.notebooklm.service.SourceService;
import github.salessew.notebooklm.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notebooks/{notebookId}/sources")
public class SourceController {

    private final SourceService sourceService;
    private final UserService userService;

    public SourceController(SourceService sourceService, UserService userService) {
        this.sourceService = sourceService;
        this.userService = userService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SourceUploadResponse> uploadSource(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID notebookId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String customName
    ) {
        User currentUser = userService.getOrCreateCurrentUser(jwt);
        SourceUploadResponse response = sourceService.initiateFileUpload(currentUser, notebookId, file, customName);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header(HttpHeaders.LOCATION, response.statusUrl())
                .body(response);
    }

    @GetMapping("/{sourceId}")
    public ResponseEntity<SourceStatusResponse> getSourceStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID notebookId,
            @PathVariable UUID sourceId
    ) {
        User currentUser = userService.getOrCreateCurrentUser(jwt);
        SourceStatusResponse response = sourceService.getSourceStatus(currentUser, notebookId, sourceId);
        return ResponseEntity.ok(response);
    }
}
