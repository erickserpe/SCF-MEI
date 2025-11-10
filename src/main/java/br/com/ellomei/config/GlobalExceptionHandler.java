package br.com.ellomei.config;

import br.com.ellomei.exception.PlanLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler global de exceções para a aplicação.
 * 
 * Esta classe centraliza o tratamento de exceções, garantindo que
 * respostas HTTP adequadas sejam retornadas para o frontend.
 * 
 * Benefícios:
 * - Respostas consistentes para todas as exceções
 * - Separação de concerns (lógica de negócio vs tratamento de erros)
 * - Facilita manutenção e adição de novos handlers
 * 
 * @author ElloMEI Team
 * @since 1.0.0
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Trata exceções de limite de plano excedido.
     *
     * Quando um usuário do plano FREE tenta exceder o limite de 20 lançamentos mensais,
     * esta exceção é lançada e capturada aqui, retornando uma resposta HTTP 403 Forbidden
     * com uma mensagem clara para o frontend.
     *
     * @param ex A exceção de limite de plano excedido
     * @return ResponseEntity com status 403 e corpo JSON contendo erro e mensagem
     */
    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<Object> handlePlanLimitExceeded(PlanLimitExceededException ex) {
        logger.warn("⚠️ Limite de plano excedido: {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Limite do Plano Excedido");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    /**
     * Sobrescreve o método da classe pai para tratar erros de validação de dados (Bean Validation).
     *
     * Quando um DTO recebido pela API falha nas validações (@Valid, @NotNull, etc),
     * esta exceção é lançada. Retorna HTTP 400 Bad Request com detalhes dos campos inválidos.
     *
     * @param ex A exceção de validação
     * @param headers Headers HTTP
     * @param status Status HTTP
     * @param request Requisição web
     * @return ResponseEntity com status 400 e detalhes dos erros de validação
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        logger.warn("⚠️ Erro de validação: {} campos inválidos", ex.getBindingResult().getFieldErrorCount());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Erro de Validação");
        body.put("message", "Um ou mais campos estão inválidos");

        // Adiciona detalhes dos campos inválidos
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        body.put("errors", fieldErrors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Sobrescreve o método da classe pai para tratar erros de JSON malformado ou inválido.
     *
     * Quando o cliente envia um JSON que não pode ser parseado (sintaxe inválida,
     * tipos incompatíveis, etc), esta exceção é lançada.
     *
     * @param ex A exceção de mensagem não legível
     * @param headers Headers HTTP
     * @param status Status HTTP
     * @param request Requisição web
     * @return ResponseEntity com status 400 e mensagem de erro
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        logger.warn("⚠️ JSON inválido recebido: {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "JSON Inválido");
        body.put("message", "O corpo da requisição contém JSON inválido ou malformado");

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Trata erros de argumento ilegal (validações de negócio).
     *
     * Quando uma validação de negócio falha (ex: conta não encontrada, valor inválido),
     * esta exceção é lançada.
     *
     * @param ex A exceção de argumento ilegal
     * @return ResponseEntity com status 400 e mensagem de erro
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("⚠️ Argumento inválido: {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Argumento Inválido");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Trata todas as exceções não capturadas pelos handlers específicos.
     *
     * Este é o handler catch-all que garante que nenhuma exceção não tratada
     * exponha detalhes internos da aplicação ao cliente.
     *
     * @param ex A exceção genérica
     * @return ResponseEntity com status 500 e mensagem genérica
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        logger.error("❌ Erro interno não tratado: {}", ex.getMessage(), ex);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Erro Interno");
        body.put("message", "Ocorreu um erro inesperado. Por favor, tente novamente mais tarde.");

        // Em desenvolvimento, pode ser útil incluir a stack trace
        // Em produção, NUNCA exponha detalhes internos!
        // body.put("details", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

