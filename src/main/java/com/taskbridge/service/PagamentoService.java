package com.taskbridge.service;

import com.taskbridge.config.PagamentoProperties;
import com.taskbridge.model.enums.ContratacaoStatus;
import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.model.enums.ModoPagamento;
import com.taskbridge.model.enums.PagamentoStatus;
import com.taskbridge.model.enums.TipoNotificacao;
import com.taskbridge.model.enums.TipoRecurso;
import com.taskbridge.model.Contratacao;
import com.taskbridge.model.Pagamento;
import com.taskbridge.dto.response.PagamentoResponse;
import com.taskbridge.exception.AcessoNegadoException;
import com.taskbridge.exception.RecursoNaoEncontradoException;
import com.taskbridge.exception.RegraNegocioException;
import com.taskbridge.pagamento.GatewayPagamento;
import com.taskbridge.pagamento.GatewayPagamento.CobrancaPix;
import com.taskbridge.pagamento.GatewayPagamento.SolicitacaoCobranca;
import com.taskbridge.pagamento.GeradorQrCode;
import com.taskbridge.repository.PagamentoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Fluxo: contratação aguardando pagamento → cobrança PIX → confirmação → contratação em andamento.
 * <p>
 * {@link #confirmar}, {@link #expirar} e {@link #cancelar} são operações do sistema: nenhum endpoint
 * público as expõe. Em produção seriam chamadas por um webhook autenticado do PSP (confirmar) e pelo
 * agendador (expirar). Em desenvolvimento, pelo controller de debug, que nem existe no perfil prod.
 */
@Service
public class PagamentoService {

    private static final Set<PagamentoStatus> EM_ABERTO = Set.of(PagamentoStatus.CREATED, PagamentoStatus.PENDING);
    private static final String ALFABETO_TXID = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int TAMANHO_TXID = 25;

    private final PagamentoRepository pagamentoRepository;
    private final ContratacaoService contratacaoService;
    private final GatewayPagamento gateway;
    private final GeradorQrCode geradorQrCode;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final PagamentoProperties propriedades;
    private final Clock clock;
    private final SecureRandom aleatorio = new SecureRandom();

    public PagamentoService(PagamentoRepository pagamentoRepository, ContratacaoService contratacaoService,
                            GatewayPagamento gateway, GeradorQrCode geradorQrCode, NotificacaoService notificacoes,
                            AuditoriaService auditoria, PagamentoProperties propriedades, Clock clock) {
        this.pagamentoRepository = pagamentoRepository;
        this.contratacaoService = contratacaoService;
        this.gateway = gateway;
        this.geradorQrCode = geradorQrCode;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
        this.propriedades = propriedades;
        this.clock = clock;
    }

    /** Idempotente: se já existe cobrança válida, devolve a mesma em vez de gerar outra. */
    @Transactional
    public CobrancaGerada gerarCobranca(Long contratacaoId, Long clienteId) {
        Contratacao contratacao = contratacaoService.carregarDoCliente(contratacaoId, clienteId);
        if (contratacao.getStatus() != ContratacaoStatus.AWAITING_PAYMENT) {
            throw new RegraNegocioException("Esta contratação não está aguardando pagamento.");
        }
        Instant agora = clock.instant();
        Optional<Pagamento> vigente = cobrancaVigente(contratacaoId, agora);
        if (vigente.isPresent()) {
            return new CobrancaGerada(responder(vigente.get()), false);
        }
        Pagamento pagamento = new Pagamento(contratacao, novoTxid(), gateway.modo(), agora,
                agora.plus(propriedades.validadeCobranca()));
        CobrancaPix cobranca = gateway.criarCobranca(
                new SolicitacaoCobranca(pagamento.getTxid(), contratacao.getValor(), "CONTRATACAO " + contratacaoId));
        pagamento.registrarCobranca(cobranca.payloadCopiaECola());
        pagamentoRepository.save(pagamento);
        auditoria.registrar(EventoAuditoria.PIX_COBRANCA_CRIADA, clienteId, TipoRecurso.PAGAMENTO, pagamento.getId());
        return new CobrancaGerada(responder(pagamento), true);
    }

    @Transactional(readOnly = true)
    public PagamentoResponse buscar(Long pagamentoId, Long usuarioId) {
        return responder(carregarDeParticipante(pagamentoId, usuarioId));
    }

    @Transactional
    public PagamentoResponse confirmar(Long pagamentoId) {
        return confirmar(travar(pagamentoId));
    }

    @Transactional
    public PagamentoResponse expirar(Long pagamentoId) {
        return expirar(travar(pagamentoId));
    }

    @Transactional
    public PagamentoResponse cancelar(Long pagamentoId) {
        return cancelar(travar(pagamentoId));
    }

    private PagamentoResponse confirmar(Pagamento pagamento) {
        pagamento.confirmar(clock.instant());
        Contratacao contratacao = pagamento.getContratacao();
        String titulo = contratacao.getProjeto().getTitulo();
        String link = "/contracts/" + contratacao.getId();
        notificacoes.notificar(contratacao.getPrestador(), TipoNotificacao.PAGAMENTO_CONFIRMADO, titulo, link);
        notificacoes.notificar(contratacao.getCliente(), TipoNotificacao.PAGAMENTO_CONFIRMADO, titulo, link);
        auditoria.registrar(EventoAuditoria.PIX_PAGO, null, TipoRecurso.PAGAMENTO, pagamento.getId());
        auditoria.registrar(EventoAuditoria.CONTRATACAO_INICIADA, null, TipoRecurso.CONTRATACAO, contratacao.getId());
        return responder(pagamento);
    }

    private PagamentoResponse expirar(Pagamento pagamento) {
        if (pagamento.getStatus().emAberto()) {
            pagamento.expirar();
            auditoria.registrar(EventoAuditoria.PIX_EXPIRADO, null, TipoRecurso.PAGAMENTO, pagamento.getId());
        }
        return responder(pagamento);
    }

    private PagamentoResponse cancelar(Pagamento pagamento) {
        pagamento.cancelar();
        gateway.cancelarCobranca(pagamento.getTxid());
        auditoria.registrar(EventoAuditoria.PIX_CANCELADO, null, TipoRecurso.PAGAMENTO, pagamento.getId());
        return responder(pagamento);
    }

    /**
     * Ponto de entrada das ações de debug. Checa de novo, aqui dentro, as condições que já impediram
     * o controller de ser criado em produção: se um dia alguém expuser este método por outro caminho,
     * ele continua recusando fora do modo MOCK com debug habilitado.
     */
    @Transactional
    public PagamentoResponse simular(Long pagamentoId, Long usuarioId, AcaoSimulada acao) {
        if (!propriedades.debugHabilitado() || gateway.modo() != ModoPagamento.MOCK) {
            throw new AcessoNegadoException("Simulação de pagamento indisponível neste ambiente.");
        }
        Pagamento pagamento = travar(pagamentoId);
        if (!pagamento.getContratacao().temParticipante(usuarioId)) {
            throw new RecursoNaoEncontradoException("Pagamento");
        }
        if (!pagamento.getContratacao().ehCliente(usuarioId)) {
            throw new AcessoNegadoException("Somente o pagador pode simular o pagamento.");
        }
        auditoria.registrar(EventoAuditoria.PIX_SIMULACAO_DEBUG, usuarioId, TipoRecurso.PAGAMENTO, pagamentoId);
        return switch (acao) {
            case PAGAR -> confirmar(pagamento);
            case EXPIRAR -> expirar(pagamento);
            case CANCELAR -> cancelar(pagamento);
        };
    }

    @Transactional(readOnly = true)
    public List<Long> idsVencidos(int lote) {
        return pagamentoRepository.idsVencidos(EM_ABERTO, clock.instant(), PageRequest.of(0, lote));
    }

    private Optional<Pagamento> cobrancaVigente(Long contratacaoId, Instant agora) {
        List<Pagamento> abertos = pagamentoRepository.findByContratacaoIdAndStatusIn(contratacaoId, EM_ABERTO);
        abertos.stream().filter(pagamento -> pagamento.venceu(agora)).forEach(Pagamento::expirar);
        return abertos.stream().filter(pagamento -> pagamento.aguardandoPagamento(agora)).findFirst();
    }

    private Pagamento carregarDeParticipante(Long pagamentoId, Long usuarioId) {
        return pagamentoRepository.findById(pagamentoId)
                .filter(pagamento -> pagamento.getContratacao().temParticipante(usuarioId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento"));
    }

    private Pagamento travar(Long pagamentoId) {
        return pagamentoRepository.buscarParaAtualizacao(pagamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento"));
    }

    private PagamentoResponse responder(Pagamento pagamento) {
        String qrCode = pagamento.getStatus().emAberto() ? geradorQrCode.comoDataUri(pagamento.getPayloadPix()) : null;
        return PagamentoResponse.de(pagamento, qrCode);
    }

    private String novoTxid() {
        StringBuilder txid = new StringBuilder(TAMANHO_TXID);
        for (int i = 0; i < TAMANHO_TXID; i++) {
            txid.append(ALFABETO_TXID.charAt(aleatorio.nextInt(ALFABETO_TXID.length())));
        }
        return txid.toString();
    }

    public record CobrancaGerada(PagamentoResponse pagamento, boolean nova) {
    }

    public enum AcaoSimulada {
        PAGAR,
        EXPIRAR,
        CANCELAR
    }
}
