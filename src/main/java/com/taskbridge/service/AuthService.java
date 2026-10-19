package com.taskbridge.service;

import com.taskbridge.config.PrivacidadeProperties;
import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.model.enums.TipoRecurso;
import com.taskbridge.model.Usuario;
import com.taskbridge.dto.request.LoginRequest;
import com.taskbridge.dto.request.RegistroRequest;
import com.taskbridge.exception.ConflitoException;
import com.taskbridge.exception.CredenciaisInvalidasException;
import com.taskbridge.repository.UsuarioRepository;
import com.taskbridge.security.PoliticaSenha;
import com.taskbridge.security.UsuarioAutenticado;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TentativasLoginService tentativasLogin;
    private final AuditoriaService auditoria;
    private final PrivacidadeProperties privacidade;
    private final Clock clock;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, TentativasLoginService tentativasLogin,
                       AuditoriaService auditoria, PrivacidadeProperties privacidade, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tentativasLogin = tentativasLogin;
        this.auditoria = auditoria;
        this.privacidade = privacidade;
        this.clock = clock;
    }

    /**
     * Sem verificação de e-mail, um conflito de cadastro sempre revela que o e-mail ou o usuário existe.
     * A mensagem é única para os dois casos e o endpoint tem rate limit próprio, o que torna a
     * enumeração lenta. O login, por sua vez, não revela nada.
     */
    @Transactional
    public Usuario registrar(RegistroRequest requisicao) {
        PoliticaSenha.validar(requisicao.senha());
        String email = Usuario.normalizarEmail(requisicao.email());
        String nomeUsuario = Usuario.normalizarNomeUsuario(requisicao.nomeUsuario());
        if (usuarioRepository.existsByEmail(email) || usuarioRepository.existsByNomeUsuario(nomeUsuario)) {
            throw new ConflitoException("E-mail ou nome de usuário indisponível.");
        }
        Usuario usuario = new Usuario(requisicao.nomeCompleto(), nomeUsuario, email,
                passwordEncoder.encode(requisicao.senha()), requisicao.tipoConta(),
                new Usuario.AceiteTermos(privacidade.versaoTermos(), clock.instant()));
        usuarioRepository.save(usuario);
        auditoria.registrar(EventoAuditoria.CONTA_CRIADA, usuario.getId(), TipoRecurso.USUARIO, usuario.getId());
        return usuario;
    }

    /** Qualquer falha vira a mesma {@link CredenciaisInvalidasException}: nada distingue conta inexistente de senha errada. */
    public Authentication autenticar(LoginRequest requisicao) {
        String email = Usuario.normalizarEmail(requisicao.email());
        if (PoliticaSenha.excedeLimiteBcrypt(requisicao.senha())) {
            throw falhaDeLogin(email);
        }
        try {
            Authentication autenticacao = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, requisicao.senha()));
            Long usuarioId = ((UsuarioAutenticado) autenticacao.getPrincipal()).getId();
            tentativasLogin.registrarSucesso(usuarioId);
            auditoria.registrar(EventoAuditoria.LOGIN_SUCESSO, usuarioId);
            return autenticacao;
        } catch (LockedException e) {
            auditoria.registrar(EventoAuditoria.LOGIN_CONTA_BLOQUEADA, tentativasLogin.idDaConta(email).orElse(null));
            throw new CredenciaisInvalidasException();
        } catch (AuthenticationException e) {
            throw falhaDeLogin(email);
        }
    }

    private CredenciaisInvalidasException falhaDeLogin(String email) {
        Long usuarioId = tentativasLogin.registrarFalha(email).orElse(null);
        auditoria.registrar(EventoAuditoria.LOGIN_FALHA, usuarioId);
        return new CredenciaisInvalidasException();
    }
}
