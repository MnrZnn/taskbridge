package com.taskbridge.service;

import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.model.Usuario;
import com.taskbridge.dto.request.AlterarEmailRequest;
import com.taskbridge.dto.request.AlterarSenhaRequest;
import com.taskbridge.dto.request.AtualizarPerfilRequest;
import com.taskbridge.dto.request.LinksPerfilRequest;
import com.taskbridge.dto.response.UsuarioResponse;
import com.taskbridge.exception.ConflitoException;
import com.taskbridge.exception.RecursoNaoEncontradoException;
import com.taskbridge.exception.RegraNegocioException;
import com.taskbridge.repository.UsuarioRepository;
import com.taskbridge.security.PoliticaSenha;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dados da própria conta. Toda operação recebe o id do usuário autenticado — nunca um id vindo da URL. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, AuditoriaService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long usuarioId) {
        return UsuarioResponse.de(buscarAtivo(usuarioId));
    }

    @Transactional(readOnly = true)
    public Usuario buscarAtivo(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário"));
    }

    @Transactional
    public UsuarioResponse atualizarDados(Long usuarioId, AtualizarPerfilRequest requisicao) {
        Usuario usuario = buscarAtivo(usuarioId);
        usuario.atualizarDadosPessoais(requisicao.nomeCompleto(), requisicao.biografia(), requisicao.localizacao());
        auditoria.registrar(EventoAuditoria.PERFIL_ATUALIZADO, usuarioId);
        return UsuarioResponse.de(usuario);
    }

    @Transactional
    public UsuarioResponse alterarEmail(Long usuarioId, AlterarEmailRequest requisicao) {
        Usuario usuario = buscarAtivo(usuarioId);
        if (!usuario.isPossuiSenha()) {
            // Sem senha, nada além da sessão confirmaria que é mesmo o dono da conta.
            throw new RegraNegocioException("Defina uma senha antes de alterar o e-mail.");
        }
        confirmarSenha(usuario, requisicao.senhaAtual());
        String novoEmail = Usuario.normalizarEmail(requisicao.novoEmail());
        if (novoEmail.equals(usuario.getEmail())) {
            return UsuarioResponse.de(usuario);
        }
        if (usuarioRepository.existsByEmail(novoEmail)) {
            throw new ConflitoException("Este e-mail não está disponível.");
        }
        usuario.alterarEmail(novoEmail);
        auditoria.registrar(EventoAuditoria.EMAIL_ALTERADO, usuarioId);
        return UsuarioResponse.de(usuario);
    }

    @Transactional
    public void alterarSenha(Long usuarioId, AlterarSenhaRequest requisicao) {
        Usuario usuario = buscarAtivo(usuarioId);
        // Conta de login social sem senha: pode definir a primeira sem "senha atual".
        if (usuario.isPossuiSenha()) {
            confirmarSenha(usuario, requisicao.senhaAtual());
            if (passwordEncoder.matches(requisicao.novaSenha(), usuario.getSenhaHash())) {
                throw new RegraNegocioException("A nova senha deve ser diferente da atual.");
            }
        }
        PoliticaSenha.validar(requisicao.novaSenha());
        usuario.alterarSenha(passwordEncoder.encode(requisicao.novaSenha()));
        auditoria.registrar(EventoAuditoria.SENHA_ALTERADA, usuarioId);
    }

    @Transactional
    public UsuarioResponse definirLinks(Long usuarioId, LinksPerfilRequest requisicao) {
        Usuario usuario = buscarAtivo(usuarioId);
        usuario.redefinirLinks(requisicao.links());
        auditoria.registrar(EventoAuditoria.PERFIL_ATUALIZADO, usuarioId);
        return UsuarioResponse.de(usuario);
    }

    /** Operações sensíveis pedem a senha de novo: uma sessão esquecida aberta não basta para executá-las. */
    void confirmarSenha(Usuario usuario, String senhaInformada) {
        if (senhaInformada == null || PoliticaSenha.excedeLimiteBcrypt(senhaInformada)
                || !passwordEncoder.matches(senhaInformada, usuario.getSenhaHash())) {
            throw new RegraNegocioException("Senha atual incorreta.");
        }
    }
}
