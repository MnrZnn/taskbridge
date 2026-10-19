package com.taskbridge.model;

import com.taskbridge.model.enums.Disponibilidade;
import com.taskbridge.model.enums.ModalidadeTrabalho;
import com.taskbridge.model.enums.PerfilStatus;
import com.taskbridge.exception.RegraNegocioException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "perfil_profissional")
public class PerfilProfissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String tituloProfissional;

    @Column(length = 2000)
    private String descricao;

    @Column(nullable = false)
    private int experienciaAnos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Disponibilidade disponibilidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModalidadeTrabalho modalidadeTrabalho;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "minimo", column = @Column(name = "faixa_preco_minima", nullable = false)),
            @AttributeOverride(name = "maximo", column = @Column(name = "faixa_preco_maxima", nullable = false))
    })
    private FaixaValor faixaPreco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilStatus status;

    @ElementCollection
    @CollectionTable(name = "perfil_habilidade", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "habilidade", nullable = false, length = 40)
    private Set<String> habilidades = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    protected PerfilProfissional() {
    }

    public PerfilProfissional(Usuario usuario, DadosProfissionais dados) {
        if (!usuario.isPrestador()) {
            throw new RegraNegocioException("Somente contas de prestador possuem perfil profissional.");
        }
        this.usuario = usuario;
        this.status = PerfilStatus.ATIVO;
        atualizar(dados);
    }

    public void atualizar(DadosProfissionais dados) {
        this.tituloProfissional = dados.tituloProfissional().strip();
        this.descricao = dados.descricao();
        this.experienciaAnos = dados.experienciaAnos();
        this.disponibilidade = dados.disponibilidade();
        this.modalidadeTrabalho = dados.modalidadeTrabalho();
        this.faixaPreco = dados.faixaPreco();
        this.habilidades.clear();
        this.habilidades.addAll(Habilidades.normalizar(dados.habilidades()));
    }

    public boolean disponivelParaMatching() {
        return status == PerfilStatus.ATIVO && disponibilidade.aceitaNovosProjetos();
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getTituloProfissional() {
        return tituloProfissional;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getExperienciaAnos() {
        return experienciaAnos;
    }

    public Disponibilidade getDisponibilidade() {
        return disponibilidade;
    }

    public ModalidadeTrabalho getModalidadeTrabalho() {
        return modalidadeTrabalho;
    }

    public FaixaValor getFaixaPreco() {
        return faixaPreco;
    }

    public PerfilStatus getStatus() {
        return status;
    }

    public Set<String> getHabilidades() {
        return Collections.unmodifiableSet(habilidades);
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
