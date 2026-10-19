package com.taskbridge.controller;

import com.taskbridge.dto.request.PaginaRequest;
import com.taskbridge.dto.request.PropostaRequest;
import com.taskbridge.dto.response.ContratacaoResponse;
import com.taskbridge.dto.response.PaginaResponse;
import com.taskbridge.dto.response.PropostaResponse;
import com.taskbridge.security.UsuarioAutenticado;
import com.taskbridge.service.PropostaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@Tag(name = "Propostas")
public class PropostaController {

    private static final Sort MAIS_RECENTES = Sort.by(Sort.Direction.DESC, "criadaEm");

    private final PropostaService propostaService;

    public PropostaController(PropostaService propostaService) {
        this.propostaService = propostaService;
    }

    @PostMapping("/api/projetos/{projetoId}/propostas")
    @PreAuthorize("hasRole('PRESTADOR')")
    @Operation(summary = "Envia proposta para um projeto aberto (uma por prestador)")
    public ResponseEntity<PropostaResponse> enviar(@PathVariable Long projetoId,
                                                   @AuthenticationPrincipal UsuarioAutenticado usuario,
                                                   @Valid @RequestBody PropostaRequest requisicao) {
        PropostaResponse criada = propostaService.enviar(projetoId, usuario.getId(), requisicao);
        return ResponseEntity.created(URI.create("/api/propostas/" + criada.id())).body(criada);
    }

    @GetMapping("/api/projetos/{projetoId}/propostas")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Propostas de um projeto — apenas para o dono do projeto")
    public PaginaResponse<PropostaResponse> doProjeto(@PathVariable Long projetoId,
                                                      @AuthenticationPrincipal UsuarioAutenticado usuario,
                                                      @Valid PaginaRequest pagina) {
        return propostaService.listarDoProjeto(projetoId, usuario.getId(), pagina.ordenadoPor(MAIS_RECENTES));
    }

    @GetMapping("/api/propostas/enviadas")
    @PreAuthorize("hasRole('PRESTADOR')")
    public PaginaResponse<PropostaResponse> enviadas(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid PaginaRequest pagina) {
        return propostaService.listarEnviadas(usuario.getId(), pagina.ordenadoPor(MAIS_RECENTES));
    }

    @GetMapping("/api/propostas/recebidas")
    @PreAuthorize("hasRole('CLIENTE')")
    public PaginaResponse<PropostaResponse> recebidas(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid PaginaRequest pagina) {
        return propostaService.listarRecebidas(usuario.getId(), pagina.ordenadoPor(MAIS_RECENTES));
    }

    @PostMapping("/api/propostas/{id}/aceitar")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Aceita a proposta: cria a contratação e recusa as demais, numa única transação")
    public ResponseEntity<ContratacaoResponse> aceitar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        ContratacaoResponse contratacao = propostaService.aceitar(id, usuario.getId());
        return ResponseEntity.created(URI.create("/api/contratacoes/" + contratacao.id())).body(contratacao);
    }

    @PostMapping("/api/propostas/{id}/rejeitar")
    @PreAuthorize("hasRole('CLIENTE')")
    public PropostaResponse rejeitar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return propostaService.rejeitar(id, usuario.getId());
    }

    @PostMapping("/api/propostas/{id}/cancelar")
    @PreAuthorize("hasRole('PRESTADOR')")
    public PropostaResponse cancelar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return propostaService.cancelar(id, usuario.getId());
    }
}
