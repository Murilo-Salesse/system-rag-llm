package github.salessew.notebooklm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/notebooklm",
        "spring.ai.openai.api-key=test-key",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:4566/dummy/.well-known/jwks.json"
})
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityConfigTest.TestEndpoints.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

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
                    Map.of("sub", "admin-user", "scope", "read write")
            );
        }
    }

    @RestController
    static class TestEndpoints {
        @GetMapping("/actuator/health")
        public String health() {
            return "UP";
        }

        @GetMapping("/api/v1/protected")
        public String protectedEndpoint() {
            return "PROTECTED";
        }
    }

    @Test
    @DisplayName("Deve permitir acesso sem autenticação ao endpoint público de health")
    void deve_permitir_acesso_anonimo_a_rotas_publicas() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve rejeitar com 401 acesso a endpoints protegidos sem token")
    void deve_rejeitar_com_401_quando_sem_token() throws Exception {
        mockMvc.perform(get("/api/v1/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve permitir acesso a endpoints protegidos quando JWT Bearer for válido")
    void deve_permitir_acesso_quando_jwt_valido() throws Exception {
        mockMvc.perform(get("/api/v1/protected")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());
    }
}
