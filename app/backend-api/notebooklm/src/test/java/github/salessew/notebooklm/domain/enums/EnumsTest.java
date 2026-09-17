package github.salessew.notebooklm.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnumsTest {

    @Test
    void deve_conter_todos_os_tipos_de_fontes_permitidos() {
        assertThat(SourceType.values()).containsExactlyInAnyOrder(
                SourceType.MARKDOWN,
                SourceType.DOCX,
                SourceType.WEB_URL
        );
        assertThat(SourceType.valueOf("MARKDOWN")).isEqualTo(SourceType.MARKDOWN);
    }

    @Test
    void deve_conter_todos_os_status_de_fonte() {
        assertThat(SourceStatus.values()).containsExactlyInAnyOrder(
                SourceStatus.PENDING,
                SourceStatus.PROCESSING,
                SourceStatus.READY,
                SourceStatus.FAILED
        );
        assertThat(SourceStatus.valueOf("READY")).isEqualTo(SourceStatus.READY);
    }

    @Test
    void deve_mapear_corretamente_message_role() {
        assertThat(MessageRole.USER.getValue()).isEqualTo("user");
        assertThat(MessageRole.ASSISTANT.getValue()).isEqualTo("assistant");

        assertThat(MessageRole.fromValue("user")).isEqualTo(MessageRole.USER);
        assertThat(MessageRole.fromValue("USER")).isEqualTo(MessageRole.USER);
        assertThat(MessageRole.fromValue("assistant")).isEqualTo(MessageRole.ASSISTANT);
        assertThat(MessageRole.fromValue("ASSISTANT")).isEqualTo(MessageRole.ASSISTANT);
    }

    @Test
    void deve_lancar_excecao_para_message_role_invalido() {
        assertThatThrownBy(() -> MessageRole.fromValue("system"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown message role: system");
    }
}
