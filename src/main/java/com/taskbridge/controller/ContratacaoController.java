package com.taskbridge.controller;

import com.taskbridge.dto.request.EnviarEntregaRequest;
import com.taskbridge.dto.request.PaginaRequest;
import com.taskbridge.dto.request.SolicitarRevisaoRequest;
import com.taskbridge.dto.response.ContratacaoResponse;
import com.taskbridge.dto.response.ContratacaoResumoResponse;
import com.taskbridge.dto.response.PagamentoResponse;
import com.taskbridge.dto.response.PaginaResponse;
import com.taskbridge.security.UsuarioAutenticado;
import com.taskbridge.service.ContratacaoService;
import com.taskbridge.service.PagamentoService;
import com.taskbridge.service.PagamentoService.CobrancaGerada;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contratacoes")
@Tag(name = "Contratações", description = "Execução: pagamento, entregas e aceite. Visível apenas aos dois participantes")
public class ContratacaoController {

    private final ContratacaoService contratacaoService;
    private final PagamentoService pagamentoService;

    public ContratacaoController(ContratacaoService contratacaoService, PagamentoService pagamentoService) {
        this.contratacaoService = contratacaoService;
        this.pagamentoService = pagamentoService;
    }

    @GetMapping
    public PaginaResponse<ContratacaoResumoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                            @Valid PaginaRequest pagina) {
        return contratacaoService.listar(usuario.getId(), pagina.ordenadoPor(Sort.by(Sort.Direction.DESC, "criadaEm")));
    }

    @GetMapping("/{id}")
    public ContratacaoResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return contratacaoService.buscar(id, usuario.getId());
    }

    @PostMapping("/{id}/pagamentos")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Gera (ou devolve a vigente) cobrança PIX. 201 se nova, 200 se já existia")
    public ResponseEntity<PagamentoResponse> gerarCobranca(@PathVariable Long id,
                                                           @AuthenticationPrincipal UsuarioAutenticado usuario) {
        CobrancaGerada cobranca = pagamentoService.gerarCobranca(id, usuario.getId());
        return ResponseEntity.status(cobranca.nova() ? HttpStatus.CREATED : HttpStatus.OK).body(cobranca.pagamento());
    }

    @PostMapping("/{id}/entregas/{entregaId}/enviar")
    @PreAuthorize("hasRole('PRESTADOR')")
    public ContratacaoResponse enviarEntrega(@PathVariable Long id, @PathVariable Long entregaId,
                                             @AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @Valid @RequestBody EnviarEntregaRequest requisicao) {
        return contratacaoService.enviarEntrega(id, entregaId, usuario.getId(), requisicao.conteudo());
    }

    @PostMapping("/{id}/entregas/{entregaId}/iniciar-revisao")
    @PreAuthorize("hasRole('CLIENTE')")
    public ContratacaoResponse iniciarRevisao(@PathVariable Long id, @PathVariable Long entregaId,
                                              @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return contratacaoService.iniciarRevisao(id, entregaId, usuario.getId());
    }

    @PostMapping("/{id}/entregas/{entregaId}/aprovar")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Aprova a entrega. A aprovação da última conclui a contratação e o projeto")
    public ContratacaoResponse aprovarEntrega(@PathVariable Long id, @PathVariable Long entregaId,
                                              @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return contratacaoService.aprovarEntrega(id, entregaId, usuario.getId());
    }

    @PostMapping("/{id}/entregas/{entregaId}/solicitar-revisao")
    @PreAuthorize("hasRole('CLIENTE')")
    public ContratacaoResponse solicitarRevisao(@PathVariable Long id, @PathVariable Long entregaId,
                                                @AuthenticationPrincipal UsuarioAutenticado usuario,
                                                @Valid @RequestBody SolicitarRevisaoRequest requisicao) {
        return contratacaoService.solicitarRevisao(id, entregaId, usuario.getId(), requisicao.feedback());
    }
}
