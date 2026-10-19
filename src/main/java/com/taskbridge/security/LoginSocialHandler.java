package com.taskbridge.security;

import com.taskbridge.model.enums.ProvedorLogin;
import com.taskbridge.service.LoginSocialService;
import com.taskbridge.service.LoginSocialService.DadosProvedor;
import com.taskbridge.service.LoginSocialService.LoginSocialRecusadoException;
import com.taskbridge.service.LoginSocialService.Resultado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Fim do fluxo OAuth. O Spring Security autentica com o token do provedor; aqui ele é trocado pelo mesmo
 * {@link UsuarioAutenticado} do login por senha, então o resto da aplicação não precisa saber como a
 * pessoa entrou. Nenhum dado do provedor é refletido na URL de retorno — só códigos fixos de erro.
 */
@Component
public class LoginSocialHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    public static final String ATRIBUTO_CADASTRO_PENDENTE = "taskbridge.cadastroSocialPendente";
    private static final Logger log = LoggerFactory.getLogger(LoginSocialHandler.class);

    private final LoginSocialService loginSocial;
    private final SessaoAutenticacao sessao;
    private final SecurityContextRepository repositorioContexto;

    public LoginSocialHandler(LoginSocialService loginSocial, SessaoAutenticacao sessao,
                              SecurityContextRepository repositorioContexto) {
        this.loginSocial = loginSocial;
        this.sessao = sessao;
        this.repositorioContexto = repositorioContexto;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication autenticacao)
            throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) autenticacao;
        DadosProvedor dados = extrair(ProvedorLogin.doIdRegistro(token.getAuthorizedClientRegistrationId()), token.getPrincipal());
        try {
            switch (loginSocial.resolver(dados)) {
                case Resultado.Entrar entrar -> {
                    UsuarioAutenticado principal = new UsuarioAutenticado(entrar.usuario(), false);
                    principal.eraseCredentials();
                    sessao.iniciar(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()),
                            request, response);
                    response.sendRedirect("/dashboard");
                }
                case Resultado.CompletarCadastro completar -> {
                    descartarAutenticacao(request, response);
                    request.getSession().setAttribute(ATRIBUTO_CADASTRO_PENDENTE, completar.pendente());
                    response.sendRedirect("/register/complete");
                }
            }
        } catch (LoginSocialRecusadoException recusado) {
            descartarAutenticacao(request, response);
            response.sendRedirect("/login?erro=" + (recusado.getMotivo() == LoginSocialRecusadoException.Motivo.EMAIL_NAO_VERIFICADO
                    ? "email-nao-verificado" : "conta-indisponivel"));
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException excecao)
            throws IOException {
        // O código de erro do OAuth (ex.: invalid_client = secret errado) diz o motivo sem expor segredos.
        String codigo = excecao instanceof OAuth2AuthenticationException oauth ? oauth.getError().getErrorCode() : "-";
        log.warn("Login social falhou: {} codigo={}", excecao.getClass().getSimpleName(), codigo);
        response.sendRedirect("/login?erro=social");
    }

    static DadosProvedor extrair(ProvedorLogin provedor, OAuth2User usuario) {
        if (provedor == ProvedorLogin.GOOGLE && usuario instanceof OidcUser google) {
            String email = google.getEmail();
            return new DadosProvedor(provedor, google.getSubject(), email, Boolean.TRUE.equals(google.getEmailVerified()),
                    google.getFullName(), email == null ? null : email.substring(0, email.indexOf('@')));
        }
        String emailVerificado = usuario.getAttribute(GithubUsuarioService.EMAIL_VERIFICADO);
        return new DadosProvedor(provedor, String.valueOf(usuario.getAttributes().get("id")), emailVerificado,
                emailVerificado != null, usuario.getAttribute("name"), usuario.getAttribute("login"));
    }

    private void descartarAutenticacao(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        repositorioContexto.saveContext(SecurityContextHolder.createEmptyContext(), request, response);
    }
}
