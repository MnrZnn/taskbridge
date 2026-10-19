package com.taskbridge.controller;

import com.taskbridge.dto.request.PaginaRequest;
import com.taskbridge.dto.response.DashboardResponse;
import com.taskbridge.dto.response.NotificacaoResponse;
import com.taskbridge.dto.response.PaginaResponse;
import com.taskbridge.security.UsuarioAutenticado;
import com.taskbridge.service.DashboardService;
import com.taskbridge.service.NotificacaoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Painel e notificações")
public class PainelController {

    private final DashboardService dashboardService;
    private final NotificacaoService notificacaoService;

    public PainelController(DashboardService dashboardService, NotificacaoService notificacaoService) {
        this.dashboardService = dashboardService;
        this.notificacaoService = notificacaoService;
    }

    @GetMapping("/api/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return dashboardService.montar(usuario.getId());
    }

    @GetMapping("/api/notificacoes")
    public PaginaResponse<NotificacaoResponse> notificacoes(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                            @Valid PaginaRequest pagina) {
        return PaginaResponse.de(notificacaoService.listar(usuario.getId(),
                pagina.ordenadoPor(Sort.by(Sort.Direction.DESC, "criadaEm"))), NotificacaoResponse::de);
    }

    @GetMapping("/api/notificacoes/nao-lidas")
    public Map<String, Long> naoLidas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return Map.of("total", notificacaoService.contarNaoLidas(usuario.getId()));
    }

    @PostMapping("/api/notificacoes/{id}/lida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLida(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        notificacaoService.marcarComoLida(id, usuario.getId());
    }

    @PostMapping("/api/notificacoes/lidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodasLidas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        notificacaoService.marcarTodasComoLidas(usuario.getId());
    }
}
