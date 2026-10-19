package com.taskbridge.model;

import com.taskbridge.model.enums.ModoPagamento;
import com.taskbridge.model.enums.PagamentoStatus;
import com.taskbridge.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cobrança PIX de uma contratação. O status nunca vem do cliente: só muda por
 * {@link #confirmar}, {@link #expirar} e {@link #cancelar}, chamados pelo serviço de pagamento.
 */
@Entity
@Table(name = "pagamento")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contratacao_id", nullable = false)
    private Contratacao contratacao;

    @Column(nullable = false, length = 35, unique = true)
    private String txid;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PagamentoStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ModoPagamento modo;

    @Column(nullable = false, length = 512)
    private String payloadPix;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant expiraEm;

    private Instant pagoEm;

    @Version
    private long version;

    protected Pagamento() {
    }

    public Pagamento(Contratacao contratacao, String txid, ModoPagamento modo, Instant agora, Instant expiraEm) {
        this.contratacao = contratacao;
        this.txid = txid;
        this.valor = contratacao.getValor();
        this.modo = modo;
        this.criadoEm = agora;
        this.expiraEm = expiraEm;
        this.status = PagamentoStatus.CREATED;
    }

    public void registrarCobranca(String payloadPix) {
        status = status.transicionarPara(PagamentoStatus.PENDING);
        this.payloadPix = payloadPix;
    }

    public void confirmar(Instant agora) {
        if (venceu(agora)) {
            throw new RegraNegocioException("Esta cobrança PIX expirou. Gere uma nova cobrança.");
        }
        status = status.transicionarPara(PagamentoStatus.PAID);
        pagoEm = agora;
        contratacao.confirmarPagamento(agora);
    }

    public void expirar() {
        status = status.transicionarPara(PagamentoStatus.EXPIRED);
    }

    public void cancelar() {
        status = status.transicionarPara(PagamentoStatus.CANCELLED);
    }

    public boolean venceu(Instant agora) {
        return status.emAberto() && !agora.isBefore(expiraEm);
    }

    public boolean aguardandoPagamento(Instant agora) {
        return status.emAberto() && agora.isBefore(expiraEm);
    }

    public Long getId() {
        return id;
    }

    public Contratacao getContratacao() {
        return contratacao;
    }

    public String getTxid() {
        return txid;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public PagamentoStatus getStatus() {
        return status;
    }

    public ModoPagamento getModo() {
        return modo;
    }

    public String getPayloadPix() {
        return payloadPix;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public Instant getPagoEm() {
        return pagoEm;
    }
}
