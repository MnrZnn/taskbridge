package com.taskbridge.security;

import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class AuditoriaLogoutHandler implements LogoutHandler {

    private final AuditoriaService auditoria;

    public AuditoriaLogoutHandler(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication autenticacao) {
        if (autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario) {
            auditoria.registrar(EventoAuditoria.LOGOUT, usuario.getId());
        }
    }
}
