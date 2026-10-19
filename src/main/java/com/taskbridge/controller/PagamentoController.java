package com.taskbridge.controller;

import com.taskbridge.dto.response.PagamentoResponse;
import com.taskbridge.security.UsuarioAutenticado;
import com.taskbridge.service.PagamentoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Só leitura. Não existe PUT/PATCH de pagamento: o status muda apenas por operações internas
 * (confirmação vinda do provedor, expiração agendada) — nunca por um JSON enviado pelo cliente.
 */
@RestController
@Tag(name = "Pagamentos PIX")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @GetMapping("/api/pagamentos/{id}")
    public PagamentoResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return pagamentoService.buscar(id, usuario.getId());
    }
}
