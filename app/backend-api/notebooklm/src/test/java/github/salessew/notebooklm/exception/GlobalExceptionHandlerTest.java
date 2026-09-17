package github.salessew.notebooklm.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler sut = new GlobalExceptionHandler();

    @Test
    void deve_tratar_resource_not_found_exception() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/notebooks/123");
        var ex = new ResourceNotFoundException("Notebook não encontrado");

        // When
        ProblemDetail result = sut.handleResourceNotFoundException(ex, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getDetail()).isEqualTo("Notebook não encontrado");
        assertThat(result.getTitle()).isEqualTo("Not Found");
        assertThat(result.getType()).isEqualTo(URI.create("https://api.notebooklm.internal/errors/not-found"));
        assertThat(result.getProperties()).containsKey("timestamp");
        assertThat(result.getProperties()).containsEntry("path", "/api/v1/notebooks/123");
    }

    @Test
    void deve_tratar_method_argument_not_valid_exception() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/notebooks");
        var bindingResult = new BeanPropertyBindingResult(new Object(), "createNotebookRequest");
        bindingResult.addError(new FieldError("createNotebookRequest", "name", "O nome é obrigatório"));
        var ex = new MethodArgumentNotValidException(null, bindingResult);

        // When
        ProblemDetail result = sut.handleValidationExceptions(ex, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getDetail()).isEqualTo("Validation failed");
        assertThat(result.getTitle()).isEqualTo("Bad Request");
        assertThat(result.getType()).isEqualTo(URI.create("https://api.notebooklm.internal/errors/bad-request"));
        assertThat(result.getProperties()).containsKey("timestamp");
        assertThat(result.getProperties()).containsEntry("path", "/api/v1/notebooks");
        assertThat(result.getProperties()).containsKey("errors");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) result.getProperties().get("errors");
        assertThat(errors).containsEntry("name", "O nome é obrigatório");
    }

    @Test
    void deve_tratar_illegal_argument_exception() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/notebooks");
        var ex = new IllegalArgumentException("Parâmetro inválido");

        // When
        ProblemDetail result = sut.handleIllegalArgumentException(ex, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getDetail()).isEqualTo("Parâmetro inválido");
        assertThat(result.getTitle()).isEqualTo("Bad Request");
        assertThat(result.getType()).isEqualTo(URI.create("https://api.notebooklm.internal/errors/bad-request"));
        assertThat(result.getProperties()).containsKey("timestamp");
        assertThat(result.getProperties()).containsEntry("path", "/api/v1/notebooks");
    }

    @Test
    void deve_tratar_general_exception() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
        var ex = new RuntimeException("Erro inesperado");

        // When
        ProblemDetail result = sut.handleGeneralException(ex, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getDetail()).isEqualTo("Erro inesperado");
        assertThat(result.getTitle()).isEqualTo("Internal Server Error");
        assertThat(result.getType()).isEqualTo(URI.create("https://api.notebooklm.internal/errors/internal-server-error"));
        assertThat(result.getProperties()).containsKey("timestamp");
        assertThat(result.getProperties()).containsEntry("path", "/api/v1/test");
    }
}
