package github.salessew.notebooklm.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void deve_instanciar_e_acessar_atributos_corretamente() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        User user = new User("sub-123", "user@example.com", "Test User");
        user.setId(id);
        user.setCreatedAt(now);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getCognitoSub()).isEqualTo("sub-123");
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getName()).isEqualTo("Test User");
        assertThat(user.getCreatedAt()).isEqualTo(now);

        user.setCognitoSub("sub-456");
        user.setEmail("user2@example.com");
        user.setName("Updated User");

        assertThat(user.getCognitoSub()).isEqualTo("sub-456");
        assertThat(user.getEmail()).isEqualTo("user2@example.com");
        assertThat(user.getName()).isEqualTo("Updated User");
    }

    @Test
    void deve_testar_equals_e_hashcode_baseado_em_cognito_sub() {
        User user1 = new User("sub-123", "user1@example.com", "User 1");
        User user2 = new User("sub-123", "user2@example.com", "User 2");
        User user3 = new User("sub-999", "user3@example.com", "User 3");

        assertThat(user1).isEqualTo(user1);
        assertThat(user1).isEqualTo(user2);
        assertThat(user1).hasSameHashCodeAs(user2);

        assertThat(user1).isNotEqualTo(user3);
        assertThat(user1).isNotEqualTo(null);
        assertThat(user1).isNotEqualTo("some string");

        User emptyUser1 = new User();
        User emptyUser2 = new User();
        assertThat(emptyUser1).isNotEqualTo(emptyUser2);

        assertThat(user1.hashCode()).isNotZero();
    }
}
