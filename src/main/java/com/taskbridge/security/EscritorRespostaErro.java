package com.taskbridge.security;

import com.taskbridge.dto.response.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filtros rodam antes do DispatcherServlet, então o {@code @RestControllerAdvice} não os alcança.
 * Esta classe garante que 401/403/413/429 vindos dos filtros tenham o mesmo formato dos demais erros.
 */
@Component
public class EscritorRespostaErro {

    private final JsonMapper jsonMapper;

    public EscritorRespostaErro(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void escrever(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String mensagem)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ErroResponse corpo = ErroResponse.de(status.value(), mensagem, request.getRequestURI());
        response.getWriter().write(jsonMapper.writeValueAsString(corpo));
    }
}
