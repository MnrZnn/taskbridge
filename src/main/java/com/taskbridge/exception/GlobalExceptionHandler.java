package com.taskbridge.exception;

import com.taskbridge.dto.response.ErroResponse;
import com.taskbridge.security.LimiteCorpoRequisicaoFilter.CorpoGrandeDemaisException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduz exceções em respostas {@link ErroResponse}. Mensagens de exceções próprias (domínio) são
 * pensadas para o usuário e podem ir para a resposta; as técnicas (SQL, Jackson, etc.) nunca vão —
 * o detalhe fica só no log do servidor.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException e, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, "Recurso não encontrado.", request);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    ResponseEntity<ErroResponse> credenciaisInvalidas(CredenciaisInvalidasException e, HttpServletRequest request) {
        return responder(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    ResponseEntity<ErroResponse> acessoNegado(AcessoNegadoException e, HttpServletRequest request) {
        return responder(HttpStatus.FORBIDDEN, e.getMessage(), request);
    }

    /** Vem de {@code @PreAuthorize}: ex. cliente tentando enviar proposta. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErroResponse> semPermissao(AccessDeniedException e, HttpServletRequest request) {
        return responder(HttpStatus.FORBIDDEN, "Seu tipo de conta não permite esta operação.", request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_CONTENT;
        return ResponseEntity.status(status)
                .body(ErroResponse.comDetalhes(status.value(), e.getMessage(), request.getRequestURI(), e.getDetalhes()));
    }

    @ExceptionHandler({TransicaoEstadoInvalidaException.class, ConflitoException.class})
    ResponseEntity<ErroResponse> conflito(TaskBridgeException e, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    /** Lock otimista/pessimista perdido ou UNIQUE violado: outra operação chegou primeiro. */
    @ExceptionHandler({ConcurrencyFailureException.class, DataIntegrityViolationException.class})
    ResponseEntity<ErroResponse> conflitoConcorrencia(RuntimeException e, HttpServletRequest request) {
        log.warn("Conflito de concorrência/integridade em {}: {}", request.getRequestURI(), e.getClass().getSimpleName());
        return responder(HttpStatus.CONFLICT, "Os dados foram alterados por outra operação. Recarregue e tente novamente.", request);
    }

    @ExceptionHandler(ArquivoInvalidoException.class)
    ResponseEntity<ErroResponse> arquivoInvalido(ArquivoInvalidoException e, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErroResponse> uploadGrande(MaxUploadSizeExceededException e, HttpServletRequest request) {
        return responder(HttpStatus.CONTENT_TOO_LARGE, "Arquivo grande demais. O limite é 2 MB.", request);
    }

    @ExceptionHandler(PagamentoIndisponivelException.class)
    ResponseEntity<ErroResponse> pagamentoIndisponivel(PagamentoIndisponivelException e, HttpServletRequest request) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), request);
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<ErroResponse> validacao(BindException e, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.isBindingFailure() ? "valor inválido" : erro.getDefaultMessage());
        }
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(ErroResponse.comCampos(status.value(), "Dados inválidos.", request.getRequestURI(), campos));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErroResponse> validacaoParametros(HandlerMethodValidationException e, HttpServletRequest request) {
        List<String> detalhes = e.getParameterValidationResults().stream()
                .map(resultado -> resultado.getMethodParameter().getParameterName() + ": "
                        + resultado.getResolvableErrors().getFirst().getDefaultMessage())
                .toList();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(ErroResponse.comDetalhes(status.value(), "Parâmetros inválidos.", request.getRequestURI(), detalhes));
    }

    /** JSON malformado ou com campo desconhecido — este último é a defesa contra mass assignment. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException e, HttpServletRequest request) {
        if (NestedExceptionUtils.getMostSpecificCause(e) instanceof CorpoGrandeDemaisException) {
            return responder(HttpStatus.CONTENT_TOO_LARGE, "Requisição grande demais.", request);
        }
        if (e.getCause() instanceof UnrecognizedPropertyException campo) {
            return responder(HttpStatus.BAD_REQUEST, "Campo não permitido: " + limitar(campo.getPropertyName()), request);
        }
        return responder(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroResponse> tipoInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + e.getName(), request);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    ResponseEntity<ErroResponse> parametroAusente(Exception e, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErroResponse> metodoNaoSuportado(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método não suportado.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErroResponse> tipoMidiaNaoSuportado(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de conteúdo não suportado.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResponse> inesperado(Exception e, HttpServletRequest request) {
        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente mais tarde.", request);
    }

    private static ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ErroResponse.de(status.value(), mensagem, request.getRequestURI()));
    }

    private static String limitar(String texto) {
        return texto == null || texto.length() <= 60 ? texto : texto.substring(0, 60);
    }
}
