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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConversationService {

    private static final String DEFAULT_SYSTEM_PROMPT = "Você é um assistente de pesquisa inteligente e prestativo baseado no NotebookLM.";

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final NotebookRepository notebookRepository;
    private final SourceRepository sourceRepository;
    private final ChatClient chatClient;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            NotebookRepository notebookRepository,
            SourceRepository sourceRepository,
            ChatClient chatClient
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.notebookRepository = notebookRepository;
        this.sourceRepository = sourceRepository;
        this.chatClient = chatClient;
    }

    @Transactional
    public ConversationResponse createConversation(User currentUser, UUID notebookId, CreateConversationRequest request) {
        Notebook notebook = findOwnedNotebookOrThrow(currentUser, notebookId);
        Conversation conversation = new Conversation(notebook);
        Conversation saved = conversationRepository.save(conversation);
        return toConversationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(User currentUser, UUID notebookId) {
        findOwnedNotebookOrThrow(currentUser, notebookId);
        return conversationRepository.findByNotebookIdOrderByCreatedAtDesc(notebookId)
                .stream()
                .map(this::toConversationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(User currentUser, UUID notebookId, UUID conversationId) {
        findOwnedNotebookOrThrow(currentUser, notebookId);

        Conversation conversation = conversationRepository.findByIdAndNotebookId(conversationId, notebookId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada"));

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public Flux<ServerSentEvent<Map<String, Object>>> streamMessage(
            User currentUser,
            UUID notebookId,
            UUID conversationId,
            StreamMessageRequest request
    ) {
        findOwnedNotebookOrThrow(currentUser, notebookId);

        Conversation conversation = conversationRepository.findByIdAndNotebookId(conversationId, notebookId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada"));

        // Se activeSourceIds fornecido, atualizar conv_active_sources
        if (request.activeSourceIds() != null && !request.activeSourceIds().isEmpty()) {
            List<Source> sources = sourceRepository.findAllById(request.activeSourceIds());
            conversation.setActiveSources(new HashSet<>(sources));
            conversationRepository.save(conversation);
        }

        // 1. Persistir mensagem do usuário imediatamente
        ConversationMessage userMessage = new ConversationMessage(conversation, MessageRole.USER, request.content());
        messageRepository.save(userMessage);

        // 2. Carregar histórico de mensagens da conversa (cronológico)
        List<ConversationMessage> messageHistory = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        // 3. Montar lista de mensagens para o ChatClient
        List<Message> promptMessages = new ArrayList<>();
        promptMessages.add(new SystemMessage(DEFAULT_SYSTEM_PROMPT));

        for (ConversationMessage msg : messageHistory) {
            if (msg.getRole() == MessageRole.USER) {
                promptMessages.add(new UserMessage(msg.getContent()));
            } else if (msg.getRole() == MessageRole.ASSISTANT) {
                promptMessages.add(new AssistantMessage(msg.getContent()));
            }
        }

        // 4. Invocar ChatClient em modo streaming e emitir SSE
        StringBuilder assistantResponseBuilder = new StringBuilder();

        Flux<ServerSentEvent<Map<String, Object>>> tokenFlux = chatClient.prompt()
                .messages(promptMessages)
                .stream()
                .content()
                .map(chunk -> {
                    assistantResponseBuilder.append(chunk);
                    return ServerSentEvent.<Map<String, Object>>builder()
                            .event("message")
                            .data(Map.of("token", chunk))
                            .build();
                });

        return tokenFlux.concatWith(Flux.defer(() -> {
            String fullContent = assistantResponseBuilder.toString();
            ConversationMessage assistantMsg = new ConversationMessage(conversation, MessageRole.ASSISTANT, fullContent);
            ConversationMessage saved = messageRepository.save(assistantMsg);

            ServerSentEvent<Map<String, Object>> doneEvent = ServerSentEvent.<Map<String, Object>>builder()
                    .event("done")
                    .data(Map.of(
                            "messageId", saved.getId().toString(),
                            "status", "COMPLETED"
                    ))
                    .build();

            return Flux.just(doneEvent);
        }));
    }

    private Notebook findOwnedNotebookOrThrow(User currentUser, UUID notebookId) {
        return notebookRepository.findByIdAndOwnerId(notebookId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notebook não encontrado"));
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getNotebook().getId(),
                conversation.getCreatedAt()
        );
    }

    private MessageResponse toMessageResponse(ConversationMessage message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getRole().getValue(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
