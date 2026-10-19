package com.taskbridge.service;

import com.taskbridge.config.PrivacidadeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

@Component
public class ManutencaoAgendada {

    private static final Logger log = LoggerFactory.getLogger(ManutencaoAgendada.class);
    private static final int LOTE_EXPIRACAO = 100;

    private final PagamentoService pagamentoService;
    private final AuditoriaService auditoriaService;
    private final PrivacidadeProperties privacidade;
    private final Clock clock;

    public ManutencaoAgendada(PagamentoService pagamentoService, AuditoriaService auditoriaService,
                              PrivacidadeProperties privacidade, Clock clock) {
        this.pagamentoService = pagamentoService;
        this.auditoriaService = auditoriaService;
        this.privacidade = privacidade;
        this.clock = clock;
    }

    /** Cada cobrança expira na própria transação: uma falha isolada não desfaz as demais. */
    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT30S")
    public void expirarCobrancasVencidas() {
        List<Long> vencidas = pagamentoService.idsVencidos(LOTE_EXPIRACAO);
        for (Long pagamentoId : vencidas) {
            try {
                pagamentoService.expirar(pagamentoId);
            } catch (RuntimeException e) {
                log.warn("Falha ao expirar pagamento {}: {}", pagamentoId, e.getClass().getSimpleName());
            }
        }
        if (!vencidas.isEmpty()) {
            log.info("{} cobrança(s) PIX expirada(s)", vencidas.size());
        }
    }

    /** Minimização (LGPD): registros de auditoria têm prazo de retenção e depois são descartados. */
    @Scheduled(cron = "0 30 3 * * *")
    public void descartarAuditoriaAntiga() {
        int removidos = auditoriaService.excluirAnterioresA(clock.instant().minus(privacidade.retencaoAuditoria()));
        log.info("{} registro(s) de auditoria além do prazo de retenção removido(s)", removidos);
    }
}
