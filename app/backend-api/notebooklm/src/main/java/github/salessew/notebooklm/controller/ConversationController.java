package github.salessew.notebooklm.controller;

import github.salessew.notebooklm.domain.entity.User;
import github.salessew.notebooklm.dto.ConversationResponse;
import github.salessew.notebooklm.dto.CreateConversationRequest;
import github.salessew.notebooklm.dto.MessageResponse;
import github.salessew.notebooklm.dto.StreamMessageRequest;
import github.salessew.notebooklm.service.ConversationService;
import github.salessew.notebooklm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notebooks/{notebookId}/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final UserService userService;

    public ConversationController(ConversationService conversationService, UserService userService) {
        this.conversationService = conversationService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID notebookId,
            @RequestBody(required = false) CreateConversationRequest request
    ) {
        User currentUser = userService.getOrCreateCurrentUser(jwt);
        ConversationResponse response = conversationService.createConversation(
                currentUser,
                notebookId,
                request != null ? request : new CreateConversationRequest()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> listConversations(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID notebookId
    ) {
        User currentUser = userService.getOrCreateCurrentUser(jwt);
        List<ConversationResponse> conversations = conversationService.listConversations(currentUser, notebookId);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> listMessages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID notebookId,
            @PathVariable UUID conversationId
    ) {
        User currentUser = userService.getOrCreateCurrentUser(jwt);
        List<MessageResponse> messages = conversationService.listMessages(currentUser, notebookId, conversationId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping(value = "/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, Object>>> streamMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID notebookId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody StreamMessageRequest request
    ) {
        User currentUser = userService.getOrCreateCurrentUser(jwt);
        return conversationService.streamMessage(currentUser, notebookId, conversationId, request);
    }
}
