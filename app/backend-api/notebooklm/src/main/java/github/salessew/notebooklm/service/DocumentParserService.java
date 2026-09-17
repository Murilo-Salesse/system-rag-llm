package github.salessew.notebooklm.service;

import github.salessew.notebooklm.domain.enums.SourceType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentParserService {

    public String parseDocument(byte[] content, SourceType type) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("O conteúdo do arquivo não pode ser vazio.");
        }

        return switch (type) {
            case MARKDOWN -> parseMarkdown(content);
            case DOCX -> parseDocx(content);
            default -> throw new IllegalArgumentException("Tipo de documento não suportado para parsing: " + type);
        };
    }

    public SourceType determineSourceType(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Nome do arquivo não pode ser vazio.");
        }

        String lower = filename.toLowerCase();
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return SourceType.MARKDOWN;
        } else if (lower.endsWith(".docx")) {
            return SourceType.DOCX;
        } else {
            throw new IllegalArgumentException("Extensão de arquivo não permitida. Apenas .md e .docx são suportados.");
        }
    }

    private String parseMarkdown(byte[] content) {
        return new String(content, StandardCharsets.UTF_8).trim();
    }

    private String parseDocx(byte[] content) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(content);
             XWPFDocument document = new XWPFDocument(bais)) {

            List<XWPFParagraph> paragraphs = document.getParagraphs();
            return paragraphs.stream()
                    .map(XWPFParagraph::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(Collectors.joining("\n\n"));

        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao ler documento Word DOCX: " + e.getMessage(), e);
        }
    }
}
