package com.taskbridge.controller;

import com.taskbridge.dto.request.PaginaRequest;
import com.taskbridge.dto.request.ServicoRequest;
import com.taskbridge.dto.response.PaginaResponse;
import com.taskbridge.dto.response.ServicoResponse;
import com.taskbridge.security.UsuarioAutenticado;
import com.taskbridge.service.ServicoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/servicos")
@Tag(name = "Serviços", description = "Serviços oferecidos por prestadores")
public class ServicoController {

    private static final Sort MAIS_RECENTES = Sort.by(Sort.Direction.DESC, "criadoEm");

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    @GetMapping
    public PaginaResponse<ServicoResponse> listar(@RequestParam(required = false) Long categoriaId, @Valid PaginaRequest pagina) {
        return servicoService.listarAtivos(categoriaId, pagina.ordenadoPor(MAIS_RECENTES));
    }

    @GetMapping("/meus")
    @PreAuthorize("hasRole('PRESTADOR')")
    public PaginaResponse<ServicoResponse> meus(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid PaginaRequest pagina) {
        return servicoService.listarDoPrestador(usuario.getId(), pagina.ordenadoPor(MAIS_RECENTES));
    }

    @GetMapping("/{id}")
    public ServicoResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicoService.buscar(id, idOuNulo(usuario));
    }

    @PostMapping
    @PreAuthorize("hasRole('PRESTADOR')")
    public ResponseEntity<ServicoResponse> criar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                 @Valid @RequestBody ServicoRequest requisicao) {
        ServicoResponse criado = servicoService.criar(usuario.getId(), requisicao);
        return ResponseEntity.created(URI.create("/api/servicos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PRESTADOR')")
    public ServicoResponse atualizar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario,
                                     @Valid @RequestBody ServicoRequest requisicao) {
        return servicoService.atualizar(id, usuario.getId(), requisicao);
    }

    @PostMapping("/{id}/pausar")
    @PreAuthorize("hasRole('PRESTADOR')")
    public ServicoResponse pausar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicoService.pausar(id, usuario.getId());
    }

    @PostMapping("/{id}/reativar")
    @PreAuthorize("hasRole('PRESTADOR')")
    public ServicoResponse reativar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicoService.reativar(id, usuario.getId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PRESTADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicoService.excluir(id, usuario.getId());
    }

    static Long idOuNulo(UsuarioAutenticado usuario) {
        return usuario == null ? null : usuario.getId();
    }
}
