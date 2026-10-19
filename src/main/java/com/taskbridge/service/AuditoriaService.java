package com.taskbridge.service;

import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.model.enums.TipoRecurso;
import com.taskbridge.model.RegistroAuditoria;
import com.taskbridge.repository.RegistroAuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.Instant;

/**
 * Participa da transação de quem chama: se o aceite de uma proposta falhar e for desfeito,
 * o registro "PROPOSTA_ACEITA" some junto, e a auditoria nunca conta algo que não aconteceu.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger("taskbridge.auditoria");

    private final RegistroAuditoriaRepository repository;
    private final Clock clock;

    public AuditoriaService(RegistroAuditoriaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void registrar(EventoAuditoria evento, Long usuarioId) {
        registrar(evento, usuarioId, null, null);
    }

    @Transactional
    public void registrar(EventoAuditoria evento, Long usuarioId, TipoRecurso recursoTipo, Long recursoId) {
        repository.save(new RegistroAuditoria(evento, usuarioId, recursoTipo, recursoId, ipDaRequisicao(), clock.instant()));
        log.info("evento={} usuario={} recurso={}:{}", evento, usuarioId, recursoTipo, recursoId);
    }

    @Transactional(readOnly = true)
    public Page<RegistroAuditoria> listarDoUsuario(Long usuarioId, Pageable pageable) {
        return repository.findByUsuarioId(usuarioId, pageable);
    }

    @Transactional
    public int excluirAnterioresA(Instant limite) {
        return repository.excluirAnterioresA(limite);
    }

    private static String ipDaRequisicao() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return atributos.getRequest().getRemoteAddr();
        }
        return null;
    }
}
