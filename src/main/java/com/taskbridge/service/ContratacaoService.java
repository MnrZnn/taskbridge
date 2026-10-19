package com.taskbridge.service;

import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.model.enums.PagamentoStatus;
import com.taskbridge.model.enums.TipoNotificacao;
import com.taskbridge.model.enums.TipoRecurso;
import com.taskbridge.model.Contratacao;
import com.taskbridge.model.Entrega;
import com.taskbridge.model.Pagamento;
import com.taskbridge.dto.response.ContratacaoResponse;
import com.taskbridge.dto.response.ContratacaoResumoResponse;
import com.taskbridge.dto.response.PaginaResponse;
import com.taskbridge.exception.AcessoNegadoException;
import com.taskbridge.exception.RecursoNaoEncontradoException;
import com.taskbridge.repository.ContratacaoRepository;
import com.taskbridge.repository.PagamentoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

/**
 * Execução da contratação: quem pode o quê. O prestador envia entregas; o cliente revisa, aprova
 * ou pede revisão. O estado (pode aprovar? pode reenviar?) é decidido pelo domínio.
 */
@Service
public class ContratacaoService {

    private static final Set<PagamentoStatus> EM_ABERTO = Set.of(PagamentoStatus.CREATED, PagamentoStatus.PENDING);

    private final ContratacaoRepository contratacaoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public ContratacaoService(ContratacaoRepository contratacaoRepository, PagamentoRepository pagamentoRepository,
                              NotificacaoService notificacoes, AuditoriaService auditoria, Clock clock) {
        this.contratacaoRepository = contratacaoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ContratacaoResumoResponse> listar(Long usuarioId, Pageable pageable) {
        return PaginaResponse.de(contratacaoRepository.buscarPorParticipante(usuarioId, pageable), ContratacaoResumoResponse::de);
    }

    @Transactional(readOnly = true)
    public ContratacaoResponse buscar(Long contratacaoId, Long usuarioId) {
        return responder(carregarDeParticipante(contratacaoId, usuarioId), usuarioId);
    }

    @Transactional
    public ContratacaoResponse enviarEntrega(Long contratacaoId, Long entregaId, Long prestadorId, String conteudo) {
        Contratacao contratacao = carregarDoPrestador(contratacaoId, prestadorId);
        contratacao.enviarEntrega(entregaId, conteudo, clock.instant());
        notificacoes.notificar(contratacao.getCliente(), TipoNotificacao.ENTREGA_ENVIADA,
                contratacao.getProjeto().getTitulo(), linkDe(contratacao));
        auditoria.registrar(EventoAuditoria.ENTREGA_ENVIADA, prestadorId, TipoRecurso.ENTREGA, entregaId);
        return responder(contratacao, prestadorId);
    }

    @Transactional
    public ContratacaoResponse iniciarRevisao(Long contratacaoId, Long entregaId, Long clienteId) {
        Contratacao contratacao = carregarDoCliente(contratacaoId, clienteId);
        contratacao.iniciarRevisao(entregaId);
        auditoria.registrar(EventoAuditoria.ENTREGA_EM_REVISAO, clienteId, TipoRecurso.ENTREGA, entregaId);
        return responder(contratacao, clienteId);
    }

    @Transactional
    public ContratacaoResponse aprovarEntrega(Long contratacaoId, Long entregaId, Long clienteId) {
        Contratacao contratacao = carregarDoCliente(contratacaoId, clienteId);
        String titulo = contratacao.getProjeto().getTitulo();
        contratacao.aprovarEntrega(entregaId, clock.instant());
        notificacoes.notificar(contratacao.getPrestador(), TipoNotificacao.ENTREGA_APROVADA, titulo, linkDe(contratacao));
        auditoria.registrar(EventoAuditoria.ENTREGA_APROVADA, clienteId, TipoRecurso.ENTREGA, entregaId);
        if (contratacao.isConcluida()) {
            notificacoes.notificar(contratacao.getPrestador(), TipoNotificacao.CONTRATACAO_CONCLUIDA, titulo, linkDe(contratacao));
            notificacoes.notificar(contratacao.getCliente(), TipoNotificacao.CONTRATACAO_CONCLUIDA, titulo, linkDe(contratacao));
            auditoria.registrar(EventoAuditoria.CONTRATACAO_CONCLUIDA, clienteId, TipoRecurso.CONTRATACAO, contratacaoId);
        }
        return responder(contratacao, clienteId);
    }

    @Transactional
    public ContratacaoResponse solicitarRevisao(Long contratacaoId, Long entregaId, Long clienteId, String feedback) {
        Contratacao contratacao = carregarDoCliente(contratacaoId, clienteId);
        Entrega entrega = contratacao.solicitarRevisao(entregaId, feedback, clock.instant());
        notificacoes.notificar(contratacao.getPrestador(), TipoNotificacao.REVISAO_SOLICITADA,
                contratacao.getProjeto().getTitulo(), linkDe(contratacao));
        auditoria.registrar(EventoAuditoria.ENTREGA_REVISAO_SOLICITADA, clienteId, TipoRecurso.ENTREGA, entrega.getId());
        return responder(contratacao, clienteId);
    }

    /** Regra 8: para quem não participa, a contratação não existe (404). */
    public Contratacao carregarDeParticipante(Long contratacaoId, Long usuarioId) {
        return contratacaoRepository.findById(contratacaoId)
                .filter(contratacao -> contratacao.temParticipante(usuarioId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contratação"));
    }

    public Contratacao carregarDoCliente(Long contratacaoId, Long clienteId) {
        Contratacao contratacao = carregarDeParticipante(contratacaoId, clienteId);
        if (!contratacao.ehCliente(clienteId)) {
            throw new AcessoNegadoException("Somente o cliente pode realizar esta operação.");
        }
        return contratacao;
    }

    private Contratacao carregarDoPrestador(Long contratacaoId, Long prestadorId) {
        Contratacao contratacao = carregarDeParticipante(contratacaoId, prestadorId);
        if (!contratacao.ehPrestador(prestadorId)) {
            throw new AcessoNegadoException("Somente o prestador pode enviar entregas.");
        }
        return contratacao;
    }

    private ContratacaoResponse responder(Contratacao contratacao, Long usuarioId) {
        Instant agora = clock.instant();
        Long pagamentoEmAberto = pagamentoRepository.findByContratacaoIdAndStatusIn(contratacao.getId(), EM_ABERTO).stream()
                .filter(pagamento -> pagamento.aguardandoPagamento(agora))
                .map(Pagamento::getId)
                .findFirst()
                .orElse(null);
        return ContratacaoResponse.de(contratacao, usuarioId, pagamentoEmAberto);
    }

    private static String linkDe(Contratacao contratacao) {
        return "/contracts/" + contratacao.getId();
    }
}
