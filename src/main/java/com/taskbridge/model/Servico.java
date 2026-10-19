package com.taskbridge.model;

import com.taskbridge.model.enums.ServicoStatus;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "servico")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Usuario prestador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precoBase;

    @Column(nullable = false)
    private int prazoDias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServicoStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant atualizadoEm;

    @Version
    private long version;

    protected Servico() {
    }

    public Servico(Usuario prestador, DadosServico dados) {
        if (!prestador.isPrestador()) {
            throw new RegraNegocioException("Somente prestadores podem oferecer serviços.");
        }
        this.prestador = prestador;
        this.status = ServicoStatus.ATIVO;
        atualizar(dados);
    }

    public void atualizar(DadosServico dados) {
        this.categoria = dados.categoria();
        this.titulo = dados.titulo().strip();
        this.descricao = dados.descricao().strip();
        this.precoBase = dados.precoBase();
        this.prazoDias = dados.prazoDias();
    }

    public void pausar() {
        this.status = ServicoStatus.PAUSADO;
    }

    public void reativar() {
        this.status = ServicoStatus.ATIVO;
    }

    public boolean pertenceA(Long usuarioId) {
        return prestador.getId().equals(usuarioId);
    }

    public boolean isAtivo() {
        return status == ServicoStatus.ATIVO;
    }

    public boolean visivelPara(Long usuarioId) {
        return isAtivo() || pertenceA(usuarioId);
    }

    public Long getId() {
        return id;
    }

    public Usuario getPrestador() {
        return prestador;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPrecoBase() {
        return precoBase;
    }

    public int getPrazoDias() {
        return prazoDias;
    }

    public ServicoStatus getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
