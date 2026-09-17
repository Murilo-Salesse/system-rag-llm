package github.salessew.notebooklm.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HelloControllerTest {

    private final HelloController controller = new HelloController();

    @Test
    void deve_retornar_200_com_dados_do_jwt() {
        Jwt jwt = Jwt.withTokenValue("mock-jwt-token")
                .header("alg", "none")
                .subject("user-uuid-123")
                .claim("email", "user@test.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        ResponseEntity<Map<String, Object>> response = controller.hello(jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Hello authenticated world!");
        assertThat(response.getBody().get("subject")).isEqualTo("user-uuid-123");
        assertThat(response.getBody().get("email")).isEqualTo("user@test.com");
        assertThat(response.getBody().get("token")).isEqualTo("mock-jwt-token");
    }

    @Test
    void deve_retornar_200_quando_jwt_nulo() {
        ResponseEntity<Map<String, Object>> response = controller.hello(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("token")).isEqualTo("anonymous");
        assertThat(response.getBody().get("subject")).isEqualTo("none");
        assertThat(response.getBody().get("email")).isEqualTo("none");
    }
}
