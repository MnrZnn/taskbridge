package com.taskbridge.service;

import com.taskbridge.model.enums.TipoNotificacao;
import com.taskbridge.model.Notificacao;
import com.taskbridge.model.Usuario;
import com.taskbridge.exception.RecursoNaoEncontradoException;
import com.taskbridge.repository.NotificacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class NotificacaoService {

    private final NotificacaoRepository repository;
    private final Clock clock;

    public NotificacaoService(NotificacaoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void notificar(Usuario destinatario, TipoNotificacao tipo, String tituloProjeto, String link) {
        if (destinatario.isAtivo()) {
            repository.save(new Notificacao(destinatario, tipo, tituloProjeto, link, clock.instant()));
        }
    }

    /** Para avisos que podem ser disparados repetidamente por outra pessoa (ex.: convites): um por destino. */
    @Transactional
    public boolean notificarUmaVez(Usuario destinatario, TipoNotificacao tipo, String tituloProjeto, String link) {
        if (repository.existsByDestinatarioIdAndTipoAndLink(destinatario.getId(), tipo, link)) {
            return false;
        }
        notificar(destinatario, tipo, tituloProjeto, link);
        return true;
    }

    @Transactional(readOnly = true)
    public Page<Notificacao> listar(Long usuarioId, Pageable pageable) {
        return repository.findByDestinatarioId(usuarioId, pageable);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Long usuarioId) {
        return repository.countByDestinatarioIdAndLidaFalse(usuarioId);
    }

    @Transactional
    public void marcarComoLida(Long notificacaoId, Long usuarioId) {
        Notificacao notificacao = repository.findById(notificacaoId)
                .filter(encontrada -> encontrada.pertenceA(usuarioId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação"));
        notificacao.marcarComoLida();
    }

    @Transactional
    public void marcarTodasComoLidas(Long usuarioId) {
        repository.marcarTodasComoLidas(usuarioId);
    }
}
