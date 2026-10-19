package com.taskbridge.service;

import com.taskbridge.config.SegurancaProperties;
import com.taskbridge.model.Usuario;
import com.taskbridge.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Contagem de falhas de login por conta. Complementa o rate limit por IP: um ataque distribuído
 * (muitos IPs, uma conta) passa pelo limite por IP, mas esbarra no bloqueio temporário da conta.
 */
@Service
public class TentativasLoginService {

    private final UsuarioRepository usuarioRepository;
    private final SegurancaProperties seguranca;
    private final Clock clock;

    public TentativasLoginService(UsuarioRepository usuarioRepository, SegurancaProperties seguranca, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.seguranca = seguranca;
        this.clock = clock;
    }

    /** @return id da conta, se existir — usado só para a auditoria, nunca na resposta ao cliente. */
    @Transactional
    public Optional<Long> registrarFalha(String email) {
        Instant agora = clock.instant();
        Optional<Usuario> usuario = usuarioRepository.findByEmail(Usuario.normalizarEmail(email)).filter(Usuario::isAtivo);
        usuario.filter(conta -> !conta.estaBloqueado(agora))
                .ifPresent(conta -> conta.registrarFalhaLogin(agora, seguranca.maxTentativasLogin(), seguranca.duracaoBloqueioLogin()));
        return usuario.map(Usuario::getId);
    }

    @Transactional
    public void registrarSucesso(Long usuarioId) {
        usuarioRepository.findById(usuarioId).ifPresent(Usuario::registrarLoginBemSucedido);
    }

    @Transactional(readOnly = true)
    public Optional<Long> idDaConta(String email) {
        return usuarioRepository.findByEmail(Usuario.normalizarEmail(email)).map(Usuario::getId);
    }
}
