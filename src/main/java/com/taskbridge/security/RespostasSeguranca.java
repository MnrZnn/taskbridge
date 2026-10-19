package com.taskbridge.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Respostas JSON para 401 (não autenticado) e 403 (sem permissão ou CSRF inválido). */
@Component
public class RespostasSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final EscritorRespostaErro escritorErro;

    public RespostasSeguranca(EscritorRespostaErro escritorErro) {
        this.escritorErro = escritorErro;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException excecao)
            throws IOException {
        escritorErro.escrever(request, response, HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException excecao)
            throws IOException {
        String mensagem = excecao instanceof CsrfException
                ? "Token CSRF ausente ou inválido. Recarregue a página e tente novamente."
                : "Você não tem permissão para esta operação.";
        escritorErro.escrever(request, response, HttpStatus.FORBIDDEN, mensagem);
    }
}
