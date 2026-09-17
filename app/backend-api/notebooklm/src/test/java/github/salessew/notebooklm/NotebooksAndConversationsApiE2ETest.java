package github.salessew.notebooklm;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.salessew.notebooklm.config.SecurityConfig;
import github.salessew.notebooklm.domain.repository.ConversationMessageRepository;
import github.salessew.notebooklm.domain.repository.ConversationRepository;
import github.salessew.notebooklm.domain.repository.NotebookRepository;
import github.salessew.notebooklm.domain.repository.UserRepository;
import github.salessew.notebooklm.dto.CreateNotebookRequest;
import github.salessew.notebooklm.dto.UpdateNotebookRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/notebooklm",
        "spring.ai.openai.api-key=test-key",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:4566/dummy/.well-known/jwks.json"
})
@AutoConfigureMockMvc
@Import({SecurityConfig.class, NotebooksAndConversationsApiE2ETest.TestSecurityBeans.class})
class NotebooksAndConversationsApiE2ETest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotebookRepository notebookRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMessageRepository messageRepository;

    @TestConfiguration
    static class TestSecurityBeans {
        @Bean
        public JwtDecoder jwtDecoder() {
            return new StubJwtDecoder();
        }
    }

    static class StubJwtDecoder implements JwtDecoder {
        @Override
        public Jwt decode(String token) throws JwtException {
            return new Jwt(
                    token,
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    Map.of("alg", "none"),
                    Map.of("sub", "user-sub-1", "email", "user1@test.com", "name", "User One")
            );
        }
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor user1Jwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(builder -> builder
                        .subject("user-sub-1")
                        .claim("email", "user1@test.com")
                        .claim("name", "User One")
                );
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor user2Jwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(builder -> builder
                        .subject("user-sub-2")
                        .claim("email", "user2@test.com")
                        .claim("name", "User Two")
                );
    }

    @Test
    @DisplayName("Fluxo completo E2E: Upsert de usuário, CRUD de notebooks e conversas com isolamento anti-IDOR")
    void fluxo_completo_e2e_com_blindagem_anti_idor() throws Exception {
        // 1. Criar Notebook como User 1
        var createRequest = new CreateNotebookRequest("IA Generativa", "Pesquisas sobre LLM");
        String notebookResponseJson = mockMvc.perform(post("/api/v1/notebooks")
                        .with(user1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("IA Generativa"))
                .andExpect(jsonPath("$.sourceCount").value(0))
                .andReturn().getResponse().getContentAsString();

        String notebookIdStr = objectMapper.readTree(notebookResponseJson).get("id").asText();
        UUID notebookId = UUID.fromString(notebookIdStr);

        // 2. Listar notebooks como User 1 (deve encontrar)
        mockMvc.perform(get("/api/v1/notebooks")
                        .with(user1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[?(@.id == '" + notebookIdStr + "')].name").value("IA Generativa"));

        // 3. Anti-IDOR: User 2 tenta obter o notebook do User 1 (deve retornar 404)
        mockMvc.perform(get("/api/v1/notebooks/{notebookId}", notebookId)
                        .with(user2Jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"));

        // 4. Anti-IDOR: User 2 tenta atualizar o notebook do User 1 (deve retornar 404)
        var updateRequest = new UpdateNotebookRequest("Hacked Name", "Hacked Desc");
        mockMvc.perform(put("/api/v1/notebooks/{notebookId}", notebookId)
                        .with(user2Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        // 5. User 1 atualiza seu próprio notebook com sucesso
        var validUpdateRequest = new UpdateNotebookRequest("IA Generativa Atualizada", "Nova Desc");
        mockMvc.perform(put("/api/v1/notebooks/{notebookId}", notebookId)
                        .with(user1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("IA Generativa Atualizada"));

        // 6. User 1 cria uma conversa no seu notebook
        String convResponseJson = mockMvc.perform(post("/api/v1/notebooks/{notebookId}/conversations", notebookId)
                        .with(user1Jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.notebookId").value(notebookIdStr))
                .andReturn().getResponse().getContentAsString();

        String convIdStr = objectMapper.readTree(convResponseJson).get("id").asText();
        UUID conversationId = UUID.fromString(convIdStr);

        // 7. Anti-IDOR: User 2 tenta listar conversas do notebook do User 1 (deve retornar 404)
        mockMvc.perform(get("/api/v1/notebooks/{notebookId}/conversations", notebookId)
                        .with(user2Jwt()))
                .andExpect(status().isNotFound());

        // 8. Anti-IDOR: User 2 tenta ler mensagens da conversa do User 1 (deve retornar 404)
        mockMvc.perform(get("/api/v1/notebooks/{notebookId}/conversations/{convId}/messages", notebookId, conversationId)
                        .with(user2Jwt()))
                .andExpect(status().isNotFound());

        // 9. User 1 lista mensagens da sua conversa (deve retornar 200 OK vazio)
        mockMvc.perform(get("/api/v1/notebooks/{notebookId}/conversations/{convId}/messages", notebookId, conversationId)
                        .with(user1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 10. User 1 exclui o notebook (204 No Content)
        mockMvc.perform(delete("/api/v1/notebooks/{notebookId}", notebookId)
                        .with(user1Jwt()))
                .andExpect(status().isNoContent());

        // 11. Verificar que o notebook foi excluído (404)
        mockMvc.perform(get("/api/v1/notebooks/{notebookId}", notebookId)
                        .with(user1Jwt()))
                .andExpect(status().isNotFound());
    }
}
