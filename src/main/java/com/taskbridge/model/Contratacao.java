package com.taskbridge.model;

import com.taskbridge.model.enums.ContratacaoStatus;
import com.taskbridge.model.enums.TipoRequisito;
import com.taskbridge.exception.RecursoNaoEncontradoException;
import com.taskbridge.exception.RegraNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Raiz do agregado de execução: entregas só mudam de estado passando por aqui,
 * porque a aprovação da última entrega conclui a contratação e o projeto.
 */
@Entity
@Table(name = "contratacao")
public class Contratacao {

    static final String TITULO_ENTREGA_PADRAO = "Entrega final do projeto";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false, unique = true)
    private Projeto projeto;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposta_id", nullable = false, unique = true)
    private Proposta proposta;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContratacaoStatus status;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    private Instant iniciadaEm;

    private Instant concluidaEm;

    @Version
    private long version;

    @OneToMany(mappedBy = "contratacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<Entrega> entregas = new ArrayList<>();

    protected Contratacao() {
    }

    Contratacao(Projeto projeto, Proposta proposta, Instant agora) {
        this.projeto = projeto;
        this.proposta = proposta;
        this.valor = proposta.getValor();
        this.status = ContratacaoStatus.AWAITING_PAYMENT;
        this.criadaEm = agora;
        planejarEntregas();
    }

    /** Cada entregável definido no escopo vira uma entrega acompanhável. */
    private void planejarEntregas() {
        List<Requisito> entregaveis = projeto.requisitosDoTipo(TipoRequisito.ENTREGAVEL);
        if (entregaveis.isEmpty()) {
            entregas.add(new Entrega(this, TITULO_ENTREGA_PADRAO, 0));
            return;
        }
        for (Requisito entregavel : entregaveis) {
            entregas.add(new Entrega(this, entregavel.getDescricao(), entregas.size()));
        }
    }

    public void confirmarPagamento(Instant agora) {
        status = status.transicionarPara(ContratacaoStatus.IN_PROGRESS);
        iniciadaEm = agora;
        projeto.iniciarExecucao();
    }

    public Entrega enviarEntrega(Long entregaId, String conteudo, Instant agora) {
        if (status != ContratacaoStatus.IN_PROGRESS) {
            throw new RegraNegocioException("Entregas só podem ser enviadas com a contratação em andamento.");
        }
        Entrega entrega = entrega(entregaId);
        entrega.enviar(conteudo, agora);
        return entrega;
    }

    public Entrega iniciarRevisao(Long entregaId) {
        Entrega entrega = entrega(entregaId);
        entrega.iniciarRevisao();
        return entrega;
    }

    public Entrega solicitarRevisao(Long entregaId, String feedback, Instant agora) {
        Entrega entrega = entrega(entregaId);
        entrega.solicitarRevisao(feedback, agora);
        return entrega;
    }

    public Entrega aprovarEntrega(Long entregaId, Instant agora) {
        Entrega entrega = entrega(entregaId);
        entrega.aprovar(agora);
        if (todasEntregasAprovadas()) {
            status = status.transicionarPara(ContratacaoStatus.COMPLETED);
            concluidaEm = agora;
            projeto.concluir();
        }
        return entrega;
    }

    private boolean todasEntregasAprovadas() {
        return entregas.stream().allMatch(Entrega::isAprovada);
    }

    private Entrega entrega(Long entregaId) {
        return entregas.stream()
                .filter(entrega -> entrega.getId().equals(entregaId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrega"));
    }

    public Usuario getCliente() {
        return projeto.getCliente();
    }

    public Usuario getPrestador() {
        return proposta.getPrestador();
    }

    public boolean ehCliente(Long usuarioId) {
        return projeto.pertenceA(usuarioId);
    }

    public boolean ehPrestador(Long usuarioId) {
        return proposta.pertenceA(usuarioId);
    }

    /** Regra 8: só as duas partes enxergam a contratação. */
    public boolean temParticipante(Long usuarioId) {
        return ehCliente(usuarioId) || ehPrestador(usuarioId);
    }

    public boolean isConcluida() {
        return status == ContratacaoStatus.COMPLETED;
    }

    public Long getId() {
        return id;
    }

    public Projeto getProjeto() {
        return projeto;
    }

    public Proposta getProposta() {
        return proposta;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public ContratacaoStatus getStatus() {
        return status;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getIniciadaEm() {
        return iniciadaEm;
    }

    public Instant getConcluidaEm() {
        return concluidaEm;
    }

    public List<Entrega> getEntregas() {
        return Collections.unmodifiableList(entregas);
    }
}
