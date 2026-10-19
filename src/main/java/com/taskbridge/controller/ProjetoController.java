package com.taskbridge.controller;

import com.taskbridge.dao.ProjetoBuscaDao.FiltroProjetos;
import com.taskbridge.dto.request.PaginaRequest;
import com.taskbridge.dto.request.ProjetoRequest;
import com.taskbridge.dto.response.CompatibilidadeResponse;
import com.taskbridge.dto.response.PaginaResponse;
import com.taskbridge.dto.response.ProjetoCompativelResponse;
import com.taskbridge.dto.response.ProjetoResponse;
import com.taskbridge.dto.response.ProjetoResumoResponse;
import com.taskbridge.security.UsuarioAutenticado;
import com.taskbridge.service.MatchingService;
import com.taskbridge.service.ProjetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
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

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/projetos")
@Tag(name = "Projetos", description = "Necessidade → escopo → publicação, e o matching de profissionais")
public class ProjetoController {

    private static final int LIMITE_PROFISSIONAIS = 10;
    private static final int LIMITE_PROJETOS_COMPATIVEIS = 20;

    private final ProjetoService projetoService;
    private final MatchingService matchingService;

    public ProjetoController(ProjetoService projetoService, MatchingService matchingService) {
        this.projetoService = projetoService;
        this.matchingService = matchingService;
    }

    @GetMapping
    @Operation(summary = "Projetos abertos a propostas, com filtros opcionais")
    public PaginaResponse<ProjetoResumoResponse> listarAbertos(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) @Size(max = 100) String texto,
            @RequestParam(required = false) @DecimalMin("0") BigDecimal orcamentoMinimo,
            @Valid PaginaRequest pagina) {
        return projetoService.listarAbertos(new FiltroProjetos(categoriaId, texto, orcamentoMinimo),
                pagina.ordenadoPor(Sort.unsorted()));
    }

    @GetMapping("/meus")
    @PreAuthorize("hasRole('CLIENTE')")
    public PaginaResponse<ProjetoResumoResponse> meus(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid PaginaRequest pagina) {
        return projetoService.listarDoCliente(usuario.getId(), pagina.ordenadoPor(Sort.by(Sort.Direction.DESC, "criadoEm")));
    }

    @GetMapping("/compativeis")
    @PreAuthorize("hasRole('PRESTADOR')")
    @Operation(summary = "Projetos abertos ordenados pela compatibilidade com o perfil do prestador")
    public List<ProjetoCompativelResponse> compativeis(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return matchingService.projetosCompativeis(usuario.getId(), LIMITE_PROJETOS_COMPATIVEIS);
    }

    @GetMapping("/{id}")
    public ProjetoResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return projetoService.buscar(id, ServicoController.idOuNulo(usuario));
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Cria o projeto como rascunho")
    public ResponseEntity<ProjetoResponse> criar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                 @Valid @RequestBody ProjetoRequest requisicao) {
        ProjetoResponse criado = projetoService.criar(usuario.getId(), requisicao);
        return ResponseEntity.created(URI.create("/api/projetos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Atualiza o escopo de um rascunho")
    public ProjetoResponse atualizar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario,
                                     @Valid @RequestBody ProjetoRequest requisicao) {
        return projetoService.atualizar(id, usuario.getId(), requisicao);
    }

    @PostMapping("/{id}/publicar")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Publica o rascunho. 422 com a lista de pendências se faltar informação")
    public ProjetoResponse publicar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return projetoService.publicar(id, usuario.getId());
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ProjetoResponse cancelar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return projetoService.cancelar(id, usuario.getId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENTE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        projetoService.excluir(id, usuario.getId());
    }

    @GetMapping("/{id}/profissionais-compativeis")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Matching: profissionais compatíveis com o projeto e a explicação de cada critério")
    public List<CompatibilidadeResponse> profissionaisCompativeis(@PathVariable Long id,
                                                                  @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return matchingService.profissionaisCompativeis(id, usuario.getId(), LIMITE_PROFISSIONAIS);
    }

    @PostMapping("/{id}/convites/{nomeUsuario}")
    @PreAuthorize("hasRole('CLIENTE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Convida um prestador a enviar proposta (gera uma notificação para ele)")
    public void convidar(@PathVariable Long id, @PathVariable String nomeUsuario,
                         @AuthenticationPrincipal UsuarioAutenticado usuario) {
        projetoService.convidarPrestador(id, usuario.getId(), nomeUsuario);
    }
}
