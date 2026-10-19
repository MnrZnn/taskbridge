package com.taskbridge.model;

import com.taskbridge.model.enums.TipoRequisito;
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

/** Requisito, entregável ou critério de aceite do projeto — o {@link TipoRequisito} diferencia. */
@Entity
@Table(name = "requisito")
public class Requisito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRequisito tipo;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(nullable = false)
    private int ordem;

    protected Requisito() {
    }

    Requisito(Projeto projeto, TipoRequisito tipo, String descricao, int ordem) {
        this.projeto = projeto;
        this.tipo = tipo;
        this.descricao = descricao;
        this.ordem = ordem;
    }

    public boolean isDoTipo(TipoRequisito tipo) {
        return this.tipo == tipo;
    }

    public Long getId() {
        return id;
    }

    public TipoRequisito getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getOrdem() {
        return ordem;
    }
}
