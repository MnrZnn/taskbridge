package com.taskbridge.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

/**
 * Faz no login via API o que o form login do Spring faria: troca o id da sessão (anti session fixation),
 * gera um novo token CSRF e grava o contexto de segurança na sessão.
 */
@Component
public class SessaoAutenticacao {

    private final SessionAuthenticationStrategy estrategiaSessao;
    private final SecurityContextRepository repositorioContexto;

    public SessaoAutenticacao(SessionAuthenticationStrategy estrategiaSessao,
                              SecurityContextRepository repositorioContexto) {
        this.estrategiaSessao = estrategiaSessao;
        this.repositorioContexto = repositorioContexto;
    }

    public void iniciar(Authentication autenticacao, HttpServletRequest request, HttpServletResponse response) {
        estrategiaSessao.onAuthentication(autenticacao, request, response);

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
        repositorioContexto.saveContext(contexto, request, response);

        // O token novo é gerado sob demanda; lê-lo aqui força o envio do cookie já na resposta do login.
        if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
            token.getToken();
        }
    }
}
