package github.salessew.notebooklm.domain.entity;

import github.salessew.notebooklm.domain.enums.MessageRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageRoleConverterTest {

    private final MessageRoleConverter converter = new MessageRoleConverter();

    @Test
    void shouldConvertToDatabaseColumn() {
        assertThat(converter.convertToDatabaseColumn(MessageRole.USER)).isEqualTo("user");
        assertThat(converter.convertToDatabaseColumn(MessageRole.ASSISTANT)).isEqualTo("assistant");
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void shouldConvertToEntityAttribute() {
        assertThat(converter.convertToEntityAttribute("user")).isEqualTo(MessageRole.USER);
        assertThat(converter.convertToEntityAttribute("assistant")).isEqualTo(MessageRole.ASSISTANT);
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
