package com.taskbridge.model;

import com.taskbridge.model.enums.TipoRequisito;
import com.taskbridge.exception.RegraNegocioException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Escopo de um projeto, montado etapa por etapa no assistente de criação.
 * Quase tudo é opcional enquanto o projeto é rascunho, por isso o Builder:
 * cada etapa preenche só o que conhece e {@link Builder#build()} valida a combinação.
 * O que é obrigatório para publicar é decidido por {@link Projeto#pendenciasParaPublicacao()}.
 */
public final class EscopoProjeto {

    public static final int MAXIMO_REQUISITOS = 30;

    private final String titulo;
    private final String necessidade;
    private final Categoria categoria;
    private final String objetivo;
    private final String descricao;
    private final FaixaValor orcamento;
    private final Integer prazoDias;
    private final Set<String> habilidades;
    private final List<ItemRequisito> requisitos;

    private EscopoProjeto(Builder builder) {
        this.titulo = builder.titulo;
        this.necessidade = builder.necessidade;
        this.categoria = builder.categoria;
        this.objetivo = builder.objetivo;
        this.descricao = builder.descricao;
        this.orcamento = builder.orcamento;
        this.prazoDias = builder.prazoDias;
        this.habilidades = Habilidades.normalizar(builder.habilidades);
        this.requisitos = List.copyOf(builder.requisitos);
    }

    public static Builder builder(String titulo, String necessidade) {
        return new Builder(titulo, necessidade);
    }

    public String titulo() {
        return titulo;
    }

    public String necessidade() {
        return necessidade;
    }

    public Categoria categoria() {
        return categoria;
    }

    public String objetivo() {
        return objetivo;
    }

    public String descricao() {
        return descricao;
    }

    public FaixaValor orcamento() {
        return orcamento;
    }

    public Integer prazoDias() {
        return prazoDias;
    }

    public Set<String> habilidades() {
        return habilidades;
    }

    public List<ItemRequisito> requisitos() {
        return requisitos;
    }

    public record ItemRequisito(TipoRequisito tipo, String descricao) {
    }

    public static final class Builder {

        private final String titulo;
        private final String necessidade;
        private Categoria categoria;
        private String objetivo;
        private String descricao;
        private FaixaValor orcamento;
        private Integer prazoDias;
        private Collection<String> habilidades = List.of();
        private final List<ItemRequisito> requisitos = new ArrayList<>();

        private Builder(String titulo, String necessidade) {
            this.titulo = obrigatorio(titulo, "Informe um título para o projeto.");
            this.necessidade = obrigatorio(necessidade, "Descreva a necessidade do projeto.");
        }

        public Builder categoria(Categoria categoria) {
            this.categoria = categoria;
            return this;
        }

        public Builder objetivo(String objetivo) {
            this.objetivo = opcional(objetivo);
            return this;
        }

        public Builder descricao(String descricao) {
            this.descricao = opcional(descricao);
            return this;
        }

        public Builder orcamento(FaixaValor orcamento) {
            this.orcamento = orcamento;
            return this;
        }

        public Builder prazoDias(Integer prazoDias) {
            this.prazoDias = prazoDias;
            return this;
        }

        public Builder habilidades(Collection<String> habilidades) {
            this.habilidades = habilidades == null ? List.of() : habilidades;
            return this;
        }

        public Builder requisito(TipoRequisito tipo, String descricao) {
            String texto = opcional(descricao);
            if (tipo != null && texto != null) {
                requisitos.add(new ItemRequisito(tipo, texto));
            }
            return this;
        }

        public EscopoProjeto build() {
            if (prazoDias != null && (prazoDias < 1 || prazoDias > 365)) {
                throw new RegraNegocioException("O prazo deve estar entre 1 e 365 dias.");
            }
            if (requisitos.size() > MAXIMO_REQUISITOS) {
                throw new RegraNegocioException("Um projeto pode ter no máximo " + MAXIMO_REQUISITOS + " itens de escopo.");
            }
            return new EscopoProjeto(this);
        }

        private static String obrigatorio(String valor, String mensagem) {
            String texto = opcional(valor);
            if (texto == null) {
                throw new RegraNegocioException(mensagem);
            }
            return texto;
        }

        private static String opcional(String valor) {
            return valor == null || valor.isBlank() ? null : valor.strip();
        }
    }
}
