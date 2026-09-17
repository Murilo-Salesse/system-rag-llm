package github.salessew.notebooklm.service;

import github.salessew.notebooklm.domain.entity.User;
import github.salessew.notebooklm.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService sut;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        sut = new UserService(userRepository);
    }

    private Jwt createJwt(String sub, String email, String name, String username) {
        var builder = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));

        if (sub != null) {
            builder.subject(sub);
        }
        if (email != null) {
            builder.claim("email", email);
        }
        if (name != null) {
            builder.claim("name", name);
        }
        if (username != null) {
            builder.claim("cognito:username", username);
        }
        return builder.build();
    }

    @Test
    void deve_lancar_excecao_quando_jwt_nulo() {
        assertThatThrownBy(() -> sut.getOrCreateCurrentUser(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT não pode ser nulo");
        verifyNoInteractions(userRepository);
    }

    @Test
    void deve_lancar_excecao_quando_sub_nulo() {
        Jwt jwt = createJwt(null, "user@test.com", "User", null);

        assertThatThrownBy(() -> sut.getOrCreateCurrentUser(jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT subject ('sub') não pode ser nulo ou vazio");
        verifyNoInteractions(userRepository);
    }

    @Test
    void deve_lancar_excecao_quando_sub_vazio() {
        Jwt jwt = createJwt("   ", "user@test.com", "User", null);

        assertThatThrownBy(() -> sut.getOrCreateCurrentUser(jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT subject ('sub') não pode ser nulo ou vazio");
        verifyNoInteractions(userRepository);
    }

    @Test
    void deve_criar_novo_usuario_quando_nao_existir() {
        // Given
        Jwt jwt = createJwt("sub-123", "alice@test.com", "Alice Smith", null);
        when(userRepository.findByCognitoSub("sub-123")).thenReturn(Optional.empty());

        User created = new User("sub-123", "alice@test.com", "Alice Smith");
        created.setId(UUID.randomUUID());
        when(userRepository.save(any(User.class))).thenReturn(created);

        // When
        User result = sut.getOrCreateCurrentUser(jwt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCognitoSub()).isEqualTo("sub-123");
        assertThat(result.getEmail()).isEqualTo("alice@test.com");
        assertThat(result.getName()).isEqualTo("Alice Smith");
        verify(userRepository).findByCognitoSub("sub-123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deve_criar_novo_usuario_com_email_fallback_e_cognito_username() {
        // Given
        Jwt jwt = createJwt("sub-456", null, null, "user_456");
        when(userRepository.findByCognitoSub("sub-456")).thenReturn(Optional.empty());

        User created = new User("sub-456", "sub-456@cognito.local", "user_456");
        created.setId(UUID.randomUUID());
        when(userRepository.save(any(User.class))).thenReturn(created);

        // When
        User result = sut.getOrCreateCurrentUser(jwt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCognitoSub()).isEqualTo("sub-456");
        assertThat(result.getEmail()).isEqualTo("sub-456@cognito.local");
        assertThat(result.getName()).isEqualTo("user_456");
        verify(userRepository).findByCognitoSub("sub-456");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deve_retornar_usuario_existente_sem_salvar_quando_dados_iguais() {
        // Given
        Jwt jwt = createJwt("sub-123", "alice@test.com", "Alice Smith", null);
        User existing = new User("sub-123", "alice@test.com", "Alice Smith");
        existing.setId(UUID.randomUUID());
        when(userRepository.findByCognitoSub("sub-123")).thenReturn(Optional.of(existing));

        // When
        User result = sut.getOrCreateCurrentUser(jwt);

        // Then
        assertThat(result).isSameAs(existing);
        verify(userRepository).findByCognitoSub("sub-123");
        verify(userRepository, never()).save(any());
    }

    @Test
    void deve_atualizar_e_salvar_usuario_quando_email_mudou() {
        // Given
        Jwt jwt = createJwt("sub-123", "alice.new@test.com", "Alice Smith", null);
        User existing = new User("sub-123", "alice.old@test.com", "Alice Smith");
        existing.setId(UUID.randomUUID());
        when(userRepository.findByCognitoSub("sub-123")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        // When
        User result = sut.getOrCreateCurrentUser(jwt);

        // Then
        assertThat(result.getEmail()).isEqualTo("alice.new@test.com");
        verify(userRepository).findByCognitoSub("sub-123");
        verify(userRepository).save(existing);
    }

    @Test
    void deve_atualizar_e_salvar_usuario_quando_nome_mudou() {
        // Given
        Jwt jwt = createJwt("sub-123", "alice@test.com", "Alice Updated", null);
        User existing = new User("sub-123", "alice@test.com", "Alice Smith");
        existing.setId(UUID.randomUUID());
        when(userRepository.findByCognitoSub("sub-123")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        // When
        User result = sut.getOrCreateCurrentUser(jwt);

        // Then
        assertThat(result.getName()).isEqualTo("Alice Updated");
        verify(userRepository).findByCognitoSub("sub-123");
        verify(userRepository).save(existing);
    }

    @Test
    void deve_atualizar_quando_email_em_branco_usa_fallback() {
        // Given
        Jwt jwt = createJwt("sub-123", "   ", "Alice Smith", null);
        User existing = new User("sub-123", "old@test.com", "Alice Smith");
        existing.setId(UUID.randomUUID());
        when(userRepository.findByCognitoSub("sub-123")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        // When
        User result = sut.getOrCreateCurrentUser(jwt);

        // Then
        assertThat(result.getEmail()).isEqualTo("sub-123@cognito.local");
        verify(userRepository).findByCognitoSub("sub-123");
        verify(userRepository).save(existing);
    }
}
