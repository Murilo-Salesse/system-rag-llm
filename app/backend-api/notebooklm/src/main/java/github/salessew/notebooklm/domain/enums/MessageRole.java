package github.salessew.notebooklm.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Papel do autor de uma mensagem na conversa.
 * O DDL define CHECK (role IN ('user', 'assistant')).
 */
public enum MessageRole {
    USER("user"),
    ASSISTANT("assistant");

    private final String value;

    MessageRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static MessageRole fromValue(String value) {
        for (MessageRole role : values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown message role: " + value);
    }
}
