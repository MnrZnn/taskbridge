package com.taskbridge.controller;

import com.taskbridge.dto.response.PagamentoResponse;
import com.taskbridge.security.UsuarioAutenticado;
import com.taskbridge.service.PagamentoService;
import com.taskbridge.service.PagamentoService.AcaoSimulada;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simula o que o provedor PIX faria. Três travas independentes:
 * <ol>
 *   <li>{@code @Profile("!prod")}: no perfil de produção a classe nem vira bean — as rotas não existem (404);</li>
 *   <li>{@code taskbridge.pagamento.debug-habilitado=true}: precisa ser ligado explicitamente;</li>
 *   <li>{@link PagamentoService#simular} confere de novo modo MOCK + debug, e que quem chama é o pagador.</li>
 * </ol>
 */
@RestController
@Profile("!prod")
@ConditionalOnProperty(prefix = "taskbridge.pagamento", name = "debug-habilitado", havingValue = "true")
@RequestMapping("/api/debug/pagamentos/{id}")
@Tag(name = "Debug PIX (somente desenvolvimento)")
public class PagamentoDebugController {

    private final PagamentoService pagamentoService;

    public PagamentoDebugController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @PostMapping("/simular-pagamento")
    @Operation(summary = "Marca a cobrança como paga, como se o provedor tivesse confirmado")
    public PagamentoResponse pagar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return pagamentoService.simular(id, usuario.getId(), AcaoSimulada.PAGAR);
    }

    @PostMapping("/simular-expiracao")
    public PagamentoResponse expirar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return pagamentoService.simular(id, usuario.getId(), AcaoSimulada.EXPIRAR);
    }

    @PostMapping("/simular-cancelamento")
    public PagamentoResponse cancelar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return pagamentoService.simular(id, usuario.getId(), AcaoSimulada.CANCELAR);
    }
}
