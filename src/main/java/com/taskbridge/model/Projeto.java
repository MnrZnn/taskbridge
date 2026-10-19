package com.taskbridge.model;

import com.taskbridge.model.enums.ProjetoStatus;
import com.taskbridge.model.enums.TipoRequisito;
import com.taskbridge.exception.RegraNegocioException;
import com.taskbridge.exception.TransicaoEstadoInvalidaException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "projeto")
public class Projeto {

    static final int TAMANHO_MINIMO_DESCRICAO = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String necessidade;

    @Column(length = 500)
    private String objetivo;

    @Column(length = 4000)
    private String descricao;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "minimo", column = @Column(name = "orcamento_minimo")),
            @AttributeOverride(name = "maximo", column = @Column(name = "orcamento_maximo"))
    })
    private FaixaValor orcamento;

    private Integer prazoDias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjetoStatus status;

    private Instant publicadoEm;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant atualizadoEm;

    @Version
    private long version;

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<Requisito> requisitos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "projeto_habilidade", joinColumns = @JoinColumn(name = "projeto_id"))
    @Column(name = "habilidade", nullable = false, length = 40)
    private Set<String> habilidades = new LinkedHashSet<>();

    protected Projeto() {
    }

    public Projeto(Usuario cliente, EscopoProjeto escopo) {
        if (!cliente.isCliente()) {
            throw new RegraNegocioException("Somente clientes podem criar projetos.");
        }
        this.cliente = cliente;
        this.status = ProjetoStatus.DRAFT;
        aplicar(escopo);
    }

    public void redefinirEscopo(EscopoProjeto escopo) {
        if (status != ProjetoStatus.DRAFT) {
            throw new RegraNegocioException("Somente projetos em rascunho podem ser editados.");
        }
        aplicar(escopo);
    }

    private void aplicar(EscopoProjeto escopo) {
        this.titulo = escopo.titulo();
        this.necessidade = escopo.necessidade();
        this.categoria = escopo.categoria();
        this.objetivo = escopo.objetivo();
        this.descricao = escopo.descricao();
        this.orcamento = escopo.orcamento();
        this.prazoDias = escopo.prazoDias();
        this.habilidades.clear();
        this.habilidades.addAll(escopo.habilidades());
        this.requisitos.clear();
        int ordem = 0;
        for (EscopoProjeto.ItemRequisito item : escopo.requisitos()) {
            requisitos.add(new Requisito(this, item.tipo(), item.descricao(), ordem++));
        }
    }

    /** Regra 10: o que falta para o projeto poder ser publicado. Lista vazia = pode publicar. */
    public List<String> pendenciasParaPublicacao() {
        List<String> pendencias = new ArrayList<>();
        if (categoria == null) {
            pendencias.add("Selecione uma categoria.");
        }
        if (objetivo == null) {
            pendencias.add("Informe o objetivo do projeto.");
        }
        if (descricao == null || descricao.length() < TAMANHO_MINIMO_DESCRICAO) {
            pendencias.add("Descreva o escopo com pelo menos " + TAMANHO_MINIMO_DESCRICAO + " caracteres.");
        }
        if (orcamento == null) {
            pendencias.add("Informe a faixa de orçamento.");
        }
        if (prazoDias == null) {
            pendencias.add("Informe o prazo desejado.");
        }
        if (requisitosDoTipo(TipoRequisito.ENTREGAVEL).isEmpty()) {
            pendencias.add("Adicione pelo menos um entregável.");
        }
        if (requisitosDoTipo(TipoRequisito.CRITERIO_ACEITE).isEmpty()) {
            pendencias.add("Adicione pelo menos um critério de aceite.");
        }
        return pendencias;
    }

    public void publicar(Instant agora) {
        List<String> pendencias = pendenciasParaPublicacao();
        if (!pendencias.isEmpty()) {
            throw new RegraNegocioException("O projeto ainda não pode ser publicado.", pendencias);
        }
        status = status.transicionarPara(ProjetoStatus.PUBLISHED);
        publicadoEm = agora;
    }

    public Proposta receberProposta(Usuario prestador, BigDecimal valor, int prazoDias, String mensagem) {
        if (pertenceA(prestador.getId())) {
            throw new RegraNegocioException("Você não pode enviar proposta para o seu próprio projeto.");
        }
        if (!prestador.isPrestador()) {
            throw new RegraNegocioException("Somente prestadores podem enviar propostas.");
        }
        if (!status.aceitaPropostas()) {
            throw new RegraNegocioException("Este projeto não está recebendo propostas.");
        }
        if (status == ProjetoStatus.PUBLISHED) {
            status = status.transicionarPara(ProjetoStatus.IN_NEGOTIATION);
        }
        return new Proposta(this, prestador, valor, prazoDias, mensagem);
    }

    /**
     * Aceitar uma proposta encerra a negociação: o projeto passa a CONTRACTED e nasce a contratação.
     * As demais propostas pendentes são recusadas pelo serviço, na mesma transação.
     */
    public Contratacao aceitarProposta(Proposta proposta, Instant agora) {
        if (!proposta.isDoProjeto(this)) {
            throw new RegraNegocioException("A proposta não pertence a este projeto.");
        }
        if (!status.permiteTransicaoPara(ProjetoStatus.CONTRACTED)) {
            throw new TransicaoEstadoInvalidaException(status, ProjetoStatus.CONTRACTED);
        }
        proposta.aceitar(agora);
        status = ProjetoStatus.CONTRACTED;
        return new Contratacao(this, proposta, agora);
    }

    void iniciarExecucao() {
        status = status.transicionarPara(ProjetoStatus.IN_PROGRESS);
    }

    void concluir() {
        status = status.transicionarPara(ProjetoStatus.COMPLETED);
    }

    public void cancelar() {
        status = status.transicionarPara(ProjetoStatus.CANCELLED);
    }

    public boolean pertenceA(Long usuarioId) {
        return cliente.getId().equals(usuarioId);
    }

    public boolean visivelPara(Long usuarioId) {
        return status.visivelPublicamente() || pertenceA(usuarioId);
    }

    public boolean podeSerExcluido() {
        return status == ProjetoStatus.DRAFT;
    }

    public List<Requisito> requisitosDoTipo(TipoRequisito tipo) {
        return requisitos.stream().filter(requisito -> requisito.isDoTipo(tipo)).toList();
    }

    public Long getId() {
        return id;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getNecessidade() {
        return necessidade;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public String getDescricao() {
        return descricao;
    }

    public FaixaValor getOrcamento() {
        return orcamento;
    }

    public Integer getPrazoDias() {
        return prazoDias;
    }

    public ProjetoStatus getStatus() {
        return status;
    }

    public Instant getPublicadoEm() {
        return publicadoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public List<Requisito> getRequisitos() {
        return Collections.unmodifiableList(requisitos);
    }

    public Set<String> getHabilidades() {
        return Collections.unmodifiableSet(habilidades);
    }
}
