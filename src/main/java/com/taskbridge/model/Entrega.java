package com.taskbridge.model;

import com.taskbridge.model.enums.EntregaStatus;
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

import java.time.Instant;

@Entity
@Table(name = "entrega")
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contratacao_id", nullable = false)
    private Contratacao contratacao;

    @Column(nullable = false, length = 500)
    private String titulo;

    @Column(nullable = false)
    private int ordem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntregaStatus status;

    @Column(length = 2000)
    private String conteudoEnvio;

    @Column(length = 1000)
    private String feedbackRevisao;

    private Instant enviadaEm;

    private Instant avaliadaEm;

    @Version
    private long version;

    protected Entrega() {
    }

    Entrega(Contratacao contratacao, String titulo, int ordem) {
        this.contratacao = contratacao;
        this.titulo = titulo;
        this.ordem = ordem;
        this.status = EntregaStatus.PENDING;
    }

    void enviar(String conteudo, Instant agora) {
        if (conteudo == null || conteudo.isBlank()) {
            throw new RegraNegocioException("Descreva o que está sendo entregue.");
        }
        status = status.transicionarPara(EntregaStatus.SUBMITTED);
        conteudoEnvio = conteudo.strip();
        enviadaEm = agora;
    }

    void iniciarRevisao() {
        status = status.transicionarPara(EntregaStatus.UNDER_REVIEW);
    }

    /** Regra 6: só aprova o que foi enviado. Regra 7: APPROVED é final no enum. */
    void aprovar(Instant agora) {
        status = status.transicionarPara(EntregaStatus.APPROVED);
        avaliadaEm = agora;
        feedbackRevisao = null;
    }

    void solicitarRevisao(String feedback, Instant agora) {
        if (feedback == null || feedback.isBlank()) {
            throw new RegraNegocioException("Explique o que precisa ser revisado.");
        }
        status = status.transicionarPara(EntregaStatus.REVISION_REQUIRED);
        feedbackRevisao = feedback.strip();
        avaliadaEm = agora;
    }

    public boolean isAprovada() {
        return status == EntregaStatus.APPROVED;
    }

    public Long getId() {
        return id;
    }

    public Contratacao getContratacao() {
        return contratacao;
    }

    public String getTitulo() {
        return titulo;
    }

    public int getOrdem() {
        return ordem;
    }

    public EntregaStatus getStatus() {
        return status;
    }

    public String getConteudoEnvio() {
        return conteudoEnvio;
    }

    public String getFeedbackRevisao() {
        return feedbackRevisao;
    }

    public Instant getEnviadaEm() {
        return enviadaEm;
    }

    public Instant getAvaliadaEm() {
        return avaliadaEm;
    }
}
