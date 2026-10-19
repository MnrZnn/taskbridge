package com.taskbridge.model.matching;

import com.taskbridge.model.Categoria;
import com.taskbridge.model.Projeto;

public class CompatibilidadeCategoria implements CriterioCompatibilidade {

    @Override
    public String nome() {
        return "Categoria";
    }

    @Override
    public AvaliacaoCriterio avaliar(Projeto projeto, CandidatoProfissional candidato) {
        Categoria categoria = projeto.getCategoria();
        if (categoria == null) {
            return AvaliacaoCriterio.naoAtendido(nome(), "Projeto sem categoria definida");
        }
        if (candidato.atendeCategoria(categoria)) {
            return AvaliacaoCriterio.atendido(nome(), "Oferece serviços em " + categoria.getNome());
        }
        return AvaliacaoCriterio.naoAtendido(nome(), "Não oferece serviços em " + categoria.getNome());
    }
}
