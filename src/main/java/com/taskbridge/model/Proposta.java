package com.taskbridge.model;

import com.taskbridge.model.enums.PropostaStatus;
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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "proposta")
public class Proposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Usuario prestador;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private int prazoDias;

    @Column(nullable = false, length = 2000)
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PropostaStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    private Instant respondidaEm;

    @Version
    private long version;

    protected Proposta() {
    }

    /** Criada apenas por {@link Projeto#receberProposta}, que valida quem pode propor. */
    Proposta(Projeto projeto, Usuario prestador, BigDecimal valor, int prazoDias, String mensagem) {
        if (valor == null || valor.signum() <= 0) {
            throw new RegraNegocioException("O valor da proposta deve ser positivo.");
        }
        if (prazoDias < 1 || prazoDias > 365) {
            throw new RegraNegocioException("O prazo da proposta deve estar entre 1 e 365 dias.");
        }
        this.projeto = projeto;
        this.prestador = prestador;
        this.valor = valor;
        this.prazoDias = prazoDias;
        this.mensagem = mensagem.strip();
        this.status = PropostaStatus.PENDING;
    }

    void aceitar(Instant agora) {
        responder(PropostaStatus.ACCEPTED, agora);
    }

    public void rejeitar(Instant agora) {
        responder(PropostaStatus.REJECTED, agora);
    }

    public void cancelar(Instant agora) {
        responder(PropostaStatus.CANCELLED, agora);
    }

    private void responder(PropostaStatus destino, Instant agora) {
        status = status.transicionarPara(destino);
        respondidaEm = agora;
    }

    public boolean isDoProjeto(Projeto outro) {
        return projeto == outro || (projeto.getId() != null && projeto.getId().equals(outro.getId()));
    }

    public boolean pertenceA(Long usuarioId) {
        return prestador.getId().equals(usuarioId);
    }

    /** Proposta é privada entre quem enviou e o dono do projeto. */
    public boolean visivelPara(Long usuarioId) {
        return pertenceA(usuarioId) || projeto.pertenceA(usuarioId);
    }

    public boolean isPendente() {
        return status == PropostaStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public Projeto getProjeto() {
        return projeto;
    }

    public Usuario getPrestador() {
        return prestador;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public int getPrazoDias() {
        return prazoDias;
    }

    public String getMensagem() {
        return mensagem;
    }

    public PropostaStatus getStatus() {
        return status;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getRespondidaEm() {
        return respondidaEm;
    }
}
