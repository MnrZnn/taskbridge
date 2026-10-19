package com.taskbridge.controller;

import com.taskbridge.model.Usuario;
import com.taskbridge.dto.request.ConcluirCadastroSocialRequest;
import com.taskbridge.dto.request.LoginRequest;
import com.taskbridge.dto.response.CadastroSocialResponse;
import com.taskbridge.exception.RecursoNaoEncontradoException;
import com.taskbridge.security.LoginSocialHandler;
import com.taskbridge.service.LoginSocialService;
import com.taskbridge.service.LoginSocialService.CadastroPendente;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.taskbridge.dto.request.RegistroRequest;
import com.taskbridge.dto.response.UsuarioResponse;
import com.taskbridge.security.SessaoAutenticacao;
import com.taskbridge.security.UsuarioAutenticado;
import com.taskbridge.service.AuthService;
import com.taskbridge.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Sessão por cookie HttpOnly + token CSRF. Logout: POST /api/auth/logout (204).")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final SessaoAutenticacao sessao;
    private final LoginSocialService loginSocialService;

    public AuthController(AuthService authService, UsuarioService usuarioService, SessaoAutenticacao sessao,
                          LoginSocialService loginSocialService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
        this.sessao = sessao;
        this.loginSocialService = loginSocialService;
    }

    @GetMapping("/csrf")
    @Operation(summary = "Devolve o token CSRF da sessão; envie-o no header X-XSRF-TOKEN em POST/PUT/DELETE")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria conta de cliente ou prestador (exige aceite dos termos)")
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest requisicao) {
        return UsuarioResponse.de(authService.registrar(requisicao));
    }

    @GetMapping("/cadastro-social")
    @Operation(summary = "Dados do login social pendente (volta do Google/GitHub sem conta existente)")
    public CadastroSocialResponse cadastroSocialPendente(HttpSession sessao) {
        return CadastroSocialResponse.de(pendente(sessao));
    }

    @PostMapping("/cadastro-social")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Conclui o cadastro via login social: tipo de conta, usuário e aceite dos termos")
    public UsuarioResponse concluirCadastroSocial(@Valid @RequestBody ConcluirCadastroSocialRequest requisicao,
                                                  HttpServletRequest request, HttpServletResponse response) {
        HttpSession sessaoHttp = request.getSession();
        Usuario usuario = loginSocialService.concluirCadastro(pendente(sessaoHttp), requisicao);
        sessaoHttp.removeAttribute(LoginSocialHandler.ATRIBUTO_CADASTRO_PENDENTE);
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario, false);
        principal.eraseCredentials();
        sessao.iniciar(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()),
                request, response);
        return UsuarioResponse.de(usuario);
    }

    private static CadastroPendente pendente(HttpSession sessao) {
        if (sessao != null && sessao.getAttribute(LoginSocialHandler.ATRIBUTO_CADASTRO_PENDENTE) instanceof CadastroPendente pendente) {
            return pendente;
        }
        throw new RecursoNaoEncontradoException("Cadastro pendente");
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e inicia a sessão. Falhas sempre retornam 401 com a mesma mensagem")
    public UsuarioResponse login(@Valid @RequestBody LoginRequest requisicao,
                                 HttpServletRequest request, HttpServletResponse response) {
        Authentication autenticacao = authService.autenticar(requisicao);
        sessao.iniciar(autenticacao, request, response);
        return usuarioService.buscar(((UsuarioAutenticado) autenticacao.getPrincipal()).getId());
    }
}
