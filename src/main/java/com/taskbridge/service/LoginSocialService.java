package com.taskbridge.service;

import com.taskbridge.config.PrivacidadeProperties;
import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.model.enums.ProvedorLogin;
import com.taskbridge.model.enums.TipoRecurso;
import com.taskbridge.model.IdentidadeExterna;
import com.taskbridge.model.Usuario;
import com.taskbridge.dto.request.ConcluirCadastroSocialRequest;
import com.taskbridge.exception.ConflitoException;
import com.taskbridge.exception.RegraNegocioException;
import com.taskbridge.repository.IdentidadeExternaRepository;
import com.taskbridge.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.io.Serializable;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Decide o que fazer quando alguém volta do Google/GitHub:
 * <ol>
 *   <li>identidade já vinculada → entra na conta;</li>
 *   <li>e-mail <b>verificado pelo provedor</b> igual ao de uma conta → vincula e entra;</li>
 *   <li>caso contrário → pede para completar o cadastro (tipo de conta, usuário e aceite dos termos).</li>
 * </ol>
 * E-mail não verificado nunca vincula: seria possível criar no provedor uma conta com o e-mail de outra
 * pessoa e assim entrar na conta dela no TaskBridge.
 */
@Service
public class LoginSocialService {

    static final Duration VALIDADE_CADASTRO_PENDENTE = Duration.ofMinutes(15);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final IdentidadeExternaRepository identidades;
    private final UsuarioRepository usuarios;
    private final TentativasLoginService tentativasLogin;
    private final AuditoriaService auditoria;
    private final PasswordEncoder passwordEncoder;
    private final PrivacidadeProperties privacidade;
    private final Clock clock;

    public LoginSocialService(IdentidadeExternaRepository identidades, UsuarioRepository usuarios,
                              TentativasLoginService tentativasLogin, AuditoriaService auditoria,
                              PasswordEncoder passwordEncoder, PrivacidadeProperties privacidade, Clock clock) {
        this.identidades = identidades;
        this.usuarios = usuarios;
        this.tentativasLogin = tentativasLogin;
        this.auditoria = auditoria;
        this.passwordEncoder = passwordEncoder;
        this.privacidade = privacidade;
        this.clock = clock;
    }

    @Transactional
    public Resultado resolver(DadosProvedor dados) {
        Optional<IdentidadeExterna> vinculada = identidades.findByProvedorAndSujeito(dados.provedor(), dados.sujeito());
        if (vinculada.isPresent()) {
            return entrar(vinculada.get().getUsuario());
        }
        if (!dados.emailVerificado()) {
            throw new LoginSocialRecusadoException(LoginSocialRecusadoException.Motivo.EMAIL_NAO_VERIFICADO);
        }
        Optional<Usuario> mesmoEmail = usuarios.findByEmail(Usuario.normalizarEmail(dados.email()));
        if (mesmoEmail.isPresent()) {
            Usuario usuario = mesmoEmail.get();
            if (!usuario.isAtivo() || identidades.existsByUsuarioIdAndProvedor(usuario.getId(), dados.provedor())) {
                throw new LoginSocialRecusadoException(LoginSocialRecusadoException.Motivo.CONTA_INDISPONIVEL);
            }
            identidades.save(new IdentidadeExterna(usuario, dados.provedor(), dados.sujeito(), clock.instant()));
            auditoria.registrar(EventoAuditoria.LOGIN_SOCIAL_VINCULADO, usuario.getId(), TipoRecurso.USUARIO, usuario.getId());
            return entrar(usuario);
        }
        return new Resultado.CompletarCadastro(new CadastroPendente(dados.provedor(), dados.sujeito(),
                Usuario.normalizarEmail(dados.email()), nomeSugerido(dados), sugerirNomeUsuario(dados.sugestaoUsuario()),
                clock.instant()));
    }

    private Resultado entrar(Usuario usuario) {
        if (!usuario.isAtivo()) {
            throw new LoginSocialRecusadoException(LoginSocialRecusadoException.Motivo.CONTA_INDISPONIVEL);
        }
        tentativasLogin.registrarSucesso(usuario.getId());
        auditoria.registrar(EventoAuditoria.LOGIN_SUCESSO, usuario.getId());
        return new Resultado.Entrar(usuario);
    }

    @Transactional
    public Usuario concluirCadastro(CadastroPendente pendente, ConcluirCadastroSocialRequest requisicao) {
        if (clock.instant().isAfter(pendente.criadoEm().plus(VALIDADE_CADASTRO_PENDENTE))) {
            throw new RegraNegocioException("O tempo para concluir o cadastro expirou. Entre novamente com o " + pendente.provedor().rotulo() + ".");
        }
        String nomeUsuario = Usuario.normalizarNomeUsuario(requisicao.nomeUsuario());
        if (usuarios.existsByEmail(pendente.email()) || usuarios.existsByNomeUsuario(nomeUsuario)
                || identidades.findByProvedorAndSujeito(pendente.provedor(), pendente.sujeito()).isPresent()) {
            throw new ConflitoException("E-mail ou nome de usuário indisponível.");
        }
        Usuario usuario = Usuario.semSenha(requisicao.nomeCompleto(), nomeUsuario, pendente.email(),
                passwordEncoder.encode(UUID.randomUUID().toString()), requisicao.tipoConta(),
                new Usuario.AceiteTermos(privacidade.versaoTermos(), clock.instant()));
        usuarios.save(usuario);
        identidades.save(new IdentidadeExterna(usuario, pendente.provedor(), pendente.sujeito(), clock.instant()));
        auditoria.registrar(EventoAuditoria.CONTA_CRIADA, usuario.getId(), TipoRecurso.USUARIO, usuario.getId());
        auditoria.registrar(EventoAuditoria.LOGIN_SUCESSO, usuario.getId());
        return usuario;
    }

    @Transactional(readOnly = true)
    public List<ProvedorLogin> vinculados(Long usuarioId) {
        return identidades.findByUsuarioId(usuarioId).stream().map(IdentidadeExterna::getProvedor).toList();
    }

    private static String nomeSugerido(DadosProvedor dados) {
        String nome = dados.nome() == null || dados.nome().isBlank() ? dados.sugestaoUsuario() : dados.nome();
        return nome == null ? "" : nome.strip().substring(0, Math.min(nome.strip().length(), 120));
    }

    /** "João.Silva@x" → "joao.silva"; se já existir, acrescenta dígitos. É só sugestão: o usuário pode trocar. */
    String sugerirNomeUsuario(String base) {
        String limpo = Normalizer.normalize(base == null ? "" : base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.]", "");
        if (limpo.length() < 3) {
            limpo = "usuario";
        }
        limpo = limpo.substring(0, Math.min(limpo.length(), 24));
        String candidato = limpo;
        for (int tentativa = 0; tentativa < 5 && usuarios.existsByNomeUsuario(candidato); tentativa++) {
            candidato = limpo + (100 + ALEATORIO.nextInt(9900));
        }
        return candidato;
    }

    /** O que interessa do usuário do provedor, já extraído do token. */
    public record DadosProvedor(ProvedorLogin provedor, String sujeito, String email, boolean emailVerificado,
                                String nome, String sugestaoUsuario) {
    }

    /** Guardado na sessão entre a volta do provedor e a tela "complete seu cadastro". */
    public record CadastroPendente(ProvedorLogin provedor, String sujeito, String email, String nomeSugerido,
                                   String usuarioSugerido, Instant criadoEm) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    public sealed interface Resultado {
        record Entrar(Usuario usuario) implements Resultado {
        }

        record CompletarCadastro(CadastroPendente pendente) implements Resultado {
        }
    }

    public static class LoginSocialRecusadoException extends RuntimeException {

        public enum Motivo { EMAIL_NAO_VERIFICADO, CONTA_INDISPONIVEL }

        private final Motivo motivo;

        public LoginSocialRecusadoException(Motivo motivo) {
            super(motivo.name());
            this.motivo = motivo;
        }

        public Motivo getMotivo() {
            return motivo;
        }
    }
}
