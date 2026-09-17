package github.salessew.notebooklm.service;

import github.salessew.notebooklm.domain.enums.SourceType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentParserServiceTest {

    private DocumentParserService sut;

    @BeforeEach
    void setUp() {
        sut = new DocumentParserService();
    }

    @Test
    void deve_determinar_tipo_markdown() {
        assertThat(sut.determineSourceType("README.md")).isEqualTo(SourceType.MARKDOWN);
        assertThat(sut.determineSourceType("document.MARKDOWN")).isEqualTo(SourceType.MARKDOWN);
    }

    @Test
    void deve_determinar_tipo_docx() {
        assertThat(sut.determineSourceType("relatorio.docx")).isEqualTo(SourceType.DOCX);
        assertThat(sut.determineSourceType("RELATORIO.DOCX")).isEqualTo(SourceType.DOCX);
    }

    @Test
    void deve_lancar_excecao_para_extensao_invalida_ou_nula() {
        assertThatThrownBy(() -> sut.determineSourceType("arquivo.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Extensão de arquivo não permitida");

        assertThatThrownBy(() -> sut.determineSourceType(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nome do arquivo não pode ser vazio");

        assertThatThrownBy(() -> sut.determineSourceType("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deve_fazer_parse_de_markdown() {
        String mdText = "# Introdução\n\nEste é um teste de RAG.";
        byte[] bytes = mdText.getBytes(StandardCharsets.UTF_8);

        String result = sut.parseDocument(bytes, SourceType.MARKDOWN);
        assertThat(result).isEqualTo(mdText);
    }

    @Test
    void deve_fazer_parse_de_docx() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p1 = doc.createParagraph();
            XWPFRun r1 = p1.createRun();
            r1.setText("Parágrafo 1 do DOCX.");

            XWPFParagraph p2 = doc.createParagraph();
            XWPFRun r2 = p2.createRun();
            r2.setText("Parágrafo 2 com mais texto.");

            doc.write(baos);
        }

        byte[] docxBytes = baos.toByteArray();
        String result = sut.parseDocument(docxBytes, SourceType.DOCX);

        assertThat(result).contains("Parágrafo 1 do DOCX.");
        assertThat(result).contains("Parágrafo 2 com mais texto.");
    }

    @Test
    void deve_lancar_excecao_quando_conteudo_vazio() {
        assertThatThrownBy(() -> sut.parseDocument(new byte[0], SourceType.MARKDOWN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conteúdo do arquivo não pode ser vazio");

        assertThatThrownBy(() -> sut.parseDocument(null, SourceType.MARKDOWN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deve_lancar_excecao_para_tipo_nao_suportado() {
        assertThatThrownBy(() -> sut.parseDocument("content".getBytes(), SourceType.WEB_URL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de documento não suportado");
    }

    @Test
    void deve_lancar_excecao_quando_docx_corrompido() {
        byte[] corruptBytes = "não é um docx válido".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> sut.parseDocument(corruptBytes, SourceType.DOCX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Falha ao ler documento Word DOCX");
    }
}
