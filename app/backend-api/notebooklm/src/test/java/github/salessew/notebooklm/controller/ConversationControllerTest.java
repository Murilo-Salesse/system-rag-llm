package github.salessew.notebooklm.controller;

import github.salessew.notebooklm.domain.entity.User;
import github.salessew.notebooklm.dto.ConversationResponse;
import github.salessew.notebooklm.dto.CreateConversationRequest;
import github.salessew.notebooklm.dto.MessageResponse;
import github.salessew.notebooklm.dto.StreamMessageRequest;
import github.salessew.notebooklm.exception.ResourceNotFoundException;
import github.salessew.notebooklm.service.ConversationService;
import github.salessew.notebooklm.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConversationControllerTest {

    private ConversationService conversationService;
    private UserService userService;
    private ConversationController sut;

    private Jwt jwt;
    private User currentUser;
    private UUID userId;
    private UUID notebookId;

    @BeforeEach
    void setUp() {
        conversationService = mock(ConversationService.class);
        userService = mock(UserService.class);
        sut = new ConversationController(conversationService, userService);

        jwt = Jwt.withTokenValue("mock-jwt-token")
                .header("alg", "none")
                .subject("sub-123")
                .build();

        userId = UUID.randomUUID();
        currentUser = new User("sub-123", "user@test.com", "User");
        currentUser.setId(userId);

        notebookId = UUID.randomUUID();
    }

    @Test
    void deve_criar_conversa_com_sucesso() {
        // Given
        CreateConversationRequest request = new CreateConversationRequest();
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);

        UUID convId = UUID.randomUUID();
        ConversationResponse response = new ConversationResponse(convId, notebookId, Instant.now());
        when(conversationService.createConversation(currentUser, notebookId, request)).thenReturn(response);

        // When
        ResponseEntity<ConversationResponse> result = sut.createConversation(jwt, notebookId, request);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(userService).getOrCreateCurrentUser(jwt);
        verify(conversationService).createConversation(currentUser, notebookId, request);
    }

    @Test
    void deve_criar_conversa_com_body_nulo() {
        // Given
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);

        UUID convId = UUID.randomUUID();
        ConversationResponse response = new ConversationResponse(convId, notebookId, Instant.now());
        when(conversationService.createConversation(eq(currentUser), eq(notebookId), any(CreateConversationRequest.class)))
                .thenReturn(response);

        // When
        ResponseEntity<ConversationResponse> result = sut.createConversation(jwt, notebookId, null);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(userService).getOrCreateCurrentUser(jwt);
        verify(conversationService).createConversation(eq(currentUser), eq(notebookId), any(CreateConversationRequest.class));
    }

    @Test
    void deve_lancar_404_ao_criar_conversa_em_notebook_de_outro_usuario() {
        // Given
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        when(conversationService.createConversation(eq(currentUser), eq(notebookId), any()))
                .thenThrow(new ResourceNotFoundException("Notebook não encontrado"));

        // When & Then
        assertThatThrownBy(() -> sut.createConversation(jwt, notebookId, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notebook não encontrado");
        verify(userService).getOrCreateCurrentUser(jwt);
    }

    @Test
    void deve_listar_conversas_com_sucesso() {
        // Given
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        ConversationResponse response = new ConversationResponse(UUID.randomUUID(), notebookId, Instant.now());
        when(conversationService.listConversations(currentUser, notebookId)).thenReturn(List.of(response));

        // When
        ResponseEntity<List<ConversationResponse>> result = sut.listConversations(jwt, notebookId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        verify(userService).getOrCreateCurrentUser(jwt);
        verify(conversationService).listConversations(currentUser, notebookId);
    }

    @Test
    void deve_lancar_404_ao_listar_conversas_de_notebook_de_outro_usuario() {
        // Given
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        when(conversationService.listConversations(currentUser, notebookId))
                .thenThrow(new ResourceNotFoundException("Notebook não encontrado"));

        // When & Then
        assertThatThrownBy(() -> sut.listConversations(jwt, notebookId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notebook não encontrado");
        verify(userService).getOrCreateCurrentUser(jwt);
    }

    @Test
    void deve_listar_mensagens_com_sucesso() {
        // Given
        UUID convId = UUID.randomUUID();
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        MessageResponse msg = new MessageResponse(UUID.randomUUID(), convId, "user", "Mensagem", Instant.now());
        when(conversationService.listMessages(currentUser, notebookId, convId)).thenReturn(List.of(msg));

        // When
        ResponseEntity<List<MessageResponse>> result = sut.listMessages(jwt, notebookId, convId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        assertThat(result.getBody().get(0).content()).isEqualTo("Mensagem");
        verify(userService).getOrCreateCurrentUser(jwt);
        verify(conversationService).listMessages(currentUser, notebookId, convId);
    }

    @Test
    void deve_lancar_404_ao_listar_mensagens_de_conversa_de_outro_usuario() {
        // Given
        UUID convId = UUID.randomUUID();
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);
        when(conversationService.listMessages(currentUser, notebookId, convId))
                .thenThrow(new ResourceNotFoundException("Conversa não encontrada"));

        // When & Then
        assertThatThrownBy(() -> sut.listMessages(jwt, notebookId, convId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conversa não encontrada");
        verify(userService).getOrCreateCurrentUser(jwt);
    }

    @Test
    void deve_fazer_stream_de_mensagens_com_sucesso() {
        // Given
        UUID convId = UUID.randomUUID();
        StreamMessageRequest request = new StreamMessageRequest("Olá mundo", null);
        when(userService.getOrCreateCurrentUser(jwt)).thenReturn(currentUser);

        ServerSentEvent<Map<String, Object>> event = ServerSentEvent.<Map<String, Object>>builder()
                .event("message")
                .data(Map.of("token", "Olá"))
                .build();
        when(conversationService.streamMessage(currentUser, notebookId, convId, request))
                .thenReturn(Flux.just(event));

        // When
        Flux<ServerSentEvent<Map<String, Object>>> result = sut.streamMessage(jwt, notebookId, convId, request);

        // Then
        List<ServerSentEvent<Map<String, Object>>> list = result.collectList().block();
        assertThat(list).isNotNull();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).event()).isEqualTo("message");
        verify(userService).getOrCreateCurrentUser(jwt);
        verify(conversationService).streamMessage(currentUser, notebookId, convId, request);
    }
}
