package github.salessew.notebooklm.service;

import github.salessew.notebooklm.domain.entity.Conversation;
import github.salessew.notebooklm.domain.entity.ConversationMessage;
import github.salessew.notebooklm.domain.entity.Notebook;
import github.salessew.notebooklm.domain.entity.Source;
import github.salessew.notebooklm.domain.entity.User;
import github.salessew.notebooklm.domain.enums.MessageRole;
import github.salessew.notebooklm.domain.repository.ConversationMessageRepository;
import github.salessew.notebooklm.domain.repository.ConversationRepository;
import github.salessew.notebooklm.domain.repository.NotebookRepository;
import github.salessew.notebooklm.domain.repository.SourceRepository;
import github.salessew.notebooklm.dto.ConversationResponse;
import github.salessew.notebooklm.dto.CreateConversationRequest;
import github.salessew.notebooklm.dto.MessageResponse;
import github.salessew.notebooklm.dto.StreamMessageRequest;
import github.salessew.notebooklm.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConversationServiceTest {

    private ConversationRepository conversationRepository;
    private ConversationMessageRepository messageRepository;
    private NotebookRepository notebookRepository;
    private SourceRepository sourceRepository;
    private ChatClient chatClient;
    private ConversationService sut;

    private User currentUser;
    private UUID userId;
    private UUID notebookId;
    private Notebook notebook;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ConversationRepository.class);
        messageRepository = mock(ConversationMessageRepository.class);
        notebookRepository = mock(NotebookRepository.class);
        sourceRepository = mock(SourceRepository.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        sut = new ConversationService(
                conversationRepository,
                messageRepository,
                notebookRepository,
                sourceRepository,
                chatClient
        );

        userId = UUID.randomUUID();
        currentUser = new User("sub-1", "user@test.com", "Test User");
        currentUser.setId(userId);

        notebookId = UUID.randomUUID();
        notebook = new Notebook(currentUser, "Notebook", "Desc");
        notebook.setId(notebookId);
    }

    @Test
    void deve_criar_conversa_com_sucesso() {
        // Given
        CreateConversationRequest request = new CreateConversationRequest();
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.of(notebook));

        UUID conversationId = UUID.randomUUID();
        Conversation savedConversation = new Conversation(notebook);
        savedConversation.setId(conversationId);
        savedConversation.setCreatedAt(Instant.now());

        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConversation);

        // When
        ConversationResponse response = sut.createConversation(currentUser, notebookId, request);

        // Then
        assertThat(response.id()).isEqualTo(conversationId);
        assertThat(response.notebookId()).isEqualTo(notebookId);
        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void deve_lancar_404_ao_criar_conversa_quando_notebook_nao_pertence_ao_usuario() {
        // Given
        CreateConversationRequest request = new CreateConversationRequest();
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sut.createConversation(currentUser, notebookId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notebook não encontrado");
        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
        verifyNoInteractions(conversationRepository);
    }

    @Test
    void deve_listar_conversas_com_sucesso() {
        // Given
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.of(notebook));

        UUID conversationId = UUID.randomUUID();
        Conversation conversation = new Conversation(notebook);
        conversation.setId(conversationId);
        conversation.setCreatedAt(Instant.now());

        when(conversationRepository.findByNotebookIdOrderByCreatedAtDesc(notebookId))
                .thenReturn(List.of(conversation));

        // When
        List<ConversationResponse> result = sut.listConversations(currentUser, notebookId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(conversationId);
        assertThat(result.get(0).notebookId()).isEqualTo(notebookId);
        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
        verify(conversationRepository).findByNotebookIdOrderByCreatedAtDesc(notebookId);
    }

    @Test
    void deve_lancar_404_ao_listar_conversas_quando_notebook_nao_pertence_ao_usuario() {
        // Given
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sut.listConversations(currentUser, notebookId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notebook não encontrado");
        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
        verifyNoInteractions(conversationRepository);
    }

    @Test
    void deve_listar_mensagens_de_conversa_com_sucesso() {
        // Given
        UUID conversationId = UUID.randomUUID();
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.of(notebook));

        Conversation conversation = new Conversation(notebook);
        conversation.setId(conversationId);
        when(conversationRepository.findByIdAndNotebookId(conversationId, notebookId))
                .thenReturn(Optional.of(conversation));

        UUID msgId = UUID.randomUUID();
        ConversationMessage message = new ConversationMessage(conversation, MessageRole.USER, "Olá");
        message.setId(msgId);
        message.setCreatedAt(Instant.now());

        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(message));

        // When
        List<MessageResponse> result = sut.listMessages(currentUser, notebookId, conversationId);

        // Then
        assertThat(result).hasSize(1);
        MessageResponse response = result.get(0);
        assertThat(response.id()).isEqualTo(msgId);
        assertThat(response.conversationId()).isEqualTo(conversationId);
        assertThat(response.role()).isEqualTo("user");
        assertThat(response.content()).isEqualTo("Olá");
        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
        verify(conversationRepository).findByIdAndNotebookId(conversationId, notebookId);
        verify(messageRepository).findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Test
    void deve_lancar_404_ao_listar_mensagens_de_conversa_quando_notebook_nao_pertence_ao_usuario() {
        // Given
        UUID conversationId = UUID.randomUUID();
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sut.listMessages(currentUser, notebookId, conversationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notebook não encontrado");
        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
        verifyNoInteractions(conversationRepository);
        verifyNoInteractions(messageRepository);
    }

    @Test
    void deve_lancar_404_ao_listar_mensagens_de_conversa_inexistente_no_notebook() {
        // Given
        UUID conversationId = UUID.randomUUID();
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.of(notebook));
        when(conversationRepository.findByIdAndNotebookId(conversationId, notebookId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sut.listMessages(currentUser, notebookId, conversationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conversa não encontrada");
        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
        verify(conversationRepository).findByIdAndNotebookId(conversationId, notebookId);
        verifyNoInteractions(messageRepository);
    }

    @Test
    void deve_fazer_stream_de_mensagem_com_sucesso() {
        // Given
        UUID conversationId = UUID.randomUUID();
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.of(notebook));

        Conversation conversation = new Conversation(notebook);
        conversation.setId(conversationId);
        when(conversationRepository.findByIdAndNotebookId(conversationId, notebookId))
                .thenReturn(Optional.of(conversation));

        UUID userMsgId = UUID.randomUUID();
        when(messageRepository.save(any(ConversationMessage.class))).thenAnswer(invocation -> {
            ConversationMessage msg = invocation.getArgument(0);
            if (msg.getId() == null) {
                msg.setId(UUID.randomUUID());
            }
            return msg;
        });

        // Simular histórico com mensagem user e assistant
        ConversationMessage oldUserMsg = new ConversationMessage(conversation, MessageRole.USER, "Mensagem antiga");
        ConversationMessage oldAssistantMsg = new ConversationMessage(conversation, MessageRole.ASSISTANT, "Resposta antiga");
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(oldUserMsg, oldAssistantMsg));

        when(chatClient.prompt().messages(anyList()).stream().content())
                .thenReturn(Flux.just("Olá", ", mundo!"));

        StreamMessageRequest request = new StreamMessageRequest("Como vai?", null);

        // When
        Flux<ServerSentEvent<Map<String, Object>>> eventFlux = sut.streamMessage(currentUser, notebookId, conversationId, request);
        List<ServerSentEvent<Map<String, Object>>> events = eventFlux.collectList().block();

        // Then
        assertThat(events).isNotNull();
        assertThat(events).hasSize(3); // 2 tokens + 1 done event
        assertThat(events.get(0).event()).isEqualTo("message");
        assertThat(events.get(0).data()).containsEntry("token", "Olá");
        assertThat(events.get(1).event()).isEqualTo("message");
        assertThat(events.get(1).data()).containsEntry("token", ", mundo!");
        assertThat(events.get(2).event()).isEqualTo("done");
        assertThat(events.get(2).data()).containsEntry("status", "COMPLETED");
        assertThat(events.get(2).data()).containsKey("messageId");

        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
        verify(conversationRepository).findByIdAndNotebookId(conversationId, notebookId);
        verify(messageRepository, atLeast(2)).save(any(ConversationMessage.class));
    }

    @Test
    void deve_atualizar_fontes_ativas_quando_informadas_na_requisicao_de_stream() {
        // Given
        UUID conversationId = UUID.randomUUID();
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.of(notebook));

        Conversation conversation = new Conversation(notebook);
        conversation.setId(conversationId);
        when(conversationRepository.findByIdAndNotebookId(conversationId, notebookId))
                .thenReturn(Optional.of(conversation));

        when(messageRepository.save(any(ConversationMessage.class))).thenAnswer(invocation -> {
            ConversationMessage msg = invocation.getArgument(0);
            if (msg.getId() == null) {
                msg.setId(UUID.randomUUID());
            }
            return msg;
        });

        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of());

        when(chatClient.prompt().messages(anyList()).stream().content())
                .thenReturn(Flux.just("Resposta"));

        UUID sourceId = UUID.randomUUID();
        Source source = new Source(notebook, "doc.md", github.salessew.notebooklm.domain.enums.SourceType.MARKDOWN, null, null);
        source.setId(sourceId);
        when(sourceRepository.findAllById(List.of(sourceId))).thenReturn(List.of(source));

        StreamMessageRequest request = new StreamMessageRequest("Pergunta", List.of(sourceId));

        // When
        List<ServerSentEvent<Map<String, Object>>> events = sut.streamMessage(currentUser, notebookId, conversationId, request)
                .collectList()
                .block();

        // Then
        assertThat(events).hasSize(2);
        verify(sourceRepository).findAllById(List.of(sourceId));
        verify(conversationRepository).save(conversation);
    }

    @Test
    void deve_lancar_404_ao_tentar_stream_quando_notebook_nao_pertence_ao_usuario() {
        // Given
        UUID conversationId = UUID.randomUUID();
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.empty());
        StreamMessageRequest request = new StreamMessageRequest("Pergunta", null);

        // When & Then
        assertThatThrownBy(() -> sut.streamMessage(currentUser, notebookId, conversationId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notebook não encontrado");
        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
        verifyNoInteractions(conversationRepository);
        verifyNoInteractions(messageRepository);
    }

    @Test
    void deve_lancar_404_ao_tentar_stream_quando_conversa_nao_encontrada() {
        // Given
        UUID conversationId = UUID.randomUUID();
        when(notebookRepository.findByIdAndOwnerId(notebookId, userId)).thenReturn(Optional.of(notebook));
        when(conversationRepository.findByIdAndNotebookId(conversationId, notebookId)).thenReturn(Optional.empty());
        StreamMessageRequest request = new StreamMessageRequest("Pergunta", null);

        // When & Then
        assertThatThrownBy(() -> sut.streamMessage(currentUser, notebookId, conversationId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conversa não encontrada");
        verify(notebookRepository).findByIdAndOwnerId(notebookId, userId);
        verify(conversationRepository).findByIdAndNotebookId(conversationId, notebookId);
        verifyNoInteractions(messageRepository);
    }
}
