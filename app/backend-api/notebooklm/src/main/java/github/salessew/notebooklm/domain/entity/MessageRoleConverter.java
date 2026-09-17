package github.salessew.notebooklm.domain.entity;

import github.salessew.notebooklm.domain.enums.MessageRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MessageRoleConverter implements AttributeConverter<MessageRole, String> {

    @Override
    public String convertToDatabaseColumn(MessageRole attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public MessageRole convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return MessageRole.fromValue(dbData);
    }
}
