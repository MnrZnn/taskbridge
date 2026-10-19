package com.taskbridge.controller;

import com.taskbridge.dto.request.AlterarEmailRequest;
import com.taskbridge.dto.request.AlterarSenhaRequest;
import com.taskbridge.dto.request.AtualizarPerfilRequest;
import com.taskbridge.model.enums.ProvedorLogin;
import com.taskbridge.dto.request.ExcluirContaRequest;
import com.taskbridge.dto.request.LinksPerfilRequest;
import com.taskbridge.dto.request.PaginaRequest;
import com.taskbridge.dto.request.PerfilProfissionalRequest;
import com.taskbridge.dto.response.ExportacaoDadosResponse;
import com.taskbridge.dto.response.PaginaResponse;
import com.taskbridge.dto.response.PerfilProfissionalResponse;
import com.taskbridge.dto.response.RegistroAuditoriaResponse;
import com.taskbridge.dto.response.UsuarioResponse;
import com.taskbridge.security.UsuarioAutenticado;
import com.taskbridge.service.AuditoriaService;
import com.taskbridge.service.ContaService;
import com.taskbridge.service.FotoPerfilService;
import com.taskbridge.service.LoginSocialService;
import com.taskbridge.service.PerfilProfissionalService;
import com.taskbridge.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Tudo em /api/me opera sobre o usuário da sessão; não existe id na URL para trocar. */
@RestController
@RequestMapping("/api/me")
@Tag(name = "Minha conta", description = "Dados pessoais, segurança, foto e direitos LGPD do usuário autenticado")
public class ContaController {

    private final UsuarioService usuarioService;
    private final FotoPerfilService fotoPerfilService;
    private final PerfilProfissionalService perfilProfissionalService;
    private final ContaService contaService;
    private final AuditoriaService auditoriaService;
    private final LoginSocialService loginSocialService;

    public ContaController(UsuarioService usuarioService, FotoPerfilService fotoPerfilService,
                           PerfilProfissionalService perfilProfissionalService, ContaService contaService,
                           AuditoriaService auditoriaService, LoginSocialService loginSocialService) {
        this.usuarioService = usuarioService;
        this.fotoPerfilService = fotoPerfilService;
        this.perfilProfissionalService = perfilProfissionalService;
        this.contaService = contaService;
        this.auditoriaService = auditoriaService;
        this.loginSocialService = loginSocialService;
    }

    @GetMapping
    public UsuarioResponse eu(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuarioService.buscar(usuario.getId());
    }

    @PutMapping
    public UsuarioResponse atualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                     @Valid @RequestBody AtualizarPerfilRequest requisicao) {
        return usuarioService.atualizarDados(usuario.getId(), requisicao);
    }

    @PutMapping("/links")
    @Operation(summary = "Substitui os links públicos do perfil (até 5, só http/https)")
    public UsuarioResponse definirLinks(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                        @Valid @RequestBody LinksPerfilRequest requisicao) {
        return usuarioService.definirLinks(usuario.getId(), requisicao);
    }

    @GetMapping("/login-social")
    @Operation(summary = "Provedores (Google/GitHub) vinculados à conta")
    public List<ProvedorLogin> loginSocial(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return loginSocialService.vinculados(usuario.getId());
    }

    @PutMapping("/email")
    @Operation(summary = "Altera o e-mail (exige a senha atual)")
    public UsuarioResponse alterarEmail(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                        @Valid @RequestBody AlterarEmailRequest requisicao) {
        return usuarioService.alterarEmail(usuario.getId(), requisicao);
    }

    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarSenha(@AuthenticationPrincipal UsuarioAutenticado usuario,
                             @Valid @RequestBody AlterarSenhaRequest requisicao) {
        usuarioService.alterarSenha(usuario.getId(), requisicao);
    }

    @PostMapping(path = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envia foto de perfil (JPG/PNG, até 2 MB). A imagem é recortada e re-codificada")
    public UsuarioResponse enviarFoto(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                      @RequestPart("arquivo") MultipartFile arquivo) {
        return fotoPerfilService.atualizar(usuario.getId(), arquivo);
    }

    @DeleteMapping("/foto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerFoto(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        fotoPerfilService.remover(usuario.getId());
    }

    @GetMapping("/perfil-profissional")
    @PreAuthorize("hasRole('PRESTADOR')")
    public ResponseEntity<PerfilProfissionalResponse> perfilProfissional(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.of(perfilProfissionalService.buscar(usuario.getId()));
    }

    @PutMapping("/perfil-profissional")
    @PreAuthorize("hasRole('PRESTADOR')")
    public PerfilProfissionalResponse salvarPerfilProfissional(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                               @Valid @RequestBody PerfilProfissionalRequest requisicao) {
        return perfilProfissionalService.salvar(usuario.getId(), requisicao);
    }

    @GetMapping("/eventos-seguranca")
    @Operation(summary = "Histórico de eventos da própria conta (logins, alterações, etc.)")
    public PaginaResponse<RegistroAuditoriaResponse> eventos(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                             @Valid PaginaRequest pagina) {
        return PaginaResponse.de(auditoriaService.listarDoUsuario(usuario.getId(),
                pagina.ordenadoPor(Sort.by(Sort.Direction.DESC, "criadoEm"))), RegistroAuditoriaResponse::de);
    }

    @GetMapping("/dados")
    @Operation(summary = "LGPD: exporta todos os dados do titular em JSON")
    public ResponseEntity<ExportacaoDadosResponse> exportar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"taskbridge-meus-dados.json\"")
                .body(contaService.exportar(usuario.getId()));
    }

    @PostMapping("/exclusao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "LGPD: exclui (anonimiza) a conta e encerra a sessão")
    public void excluirConta(@AuthenticationPrincipal UsuarioAutenticado usuario,
                             @Valid @RequestBody ExcluirContaRequest requisicao,
                             HttpServletRequest request, HttpServletResponse response) {
        contaService.excluir(usuario.getId(), requisicao);
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
    }
}
