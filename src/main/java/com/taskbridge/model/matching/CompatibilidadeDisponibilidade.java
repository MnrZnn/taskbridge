package com.taskbridge.model.matching;

import com.taskbridge.model.enums.Disponibilidade;
import com.taskbridge.model.Projeto;

public class CompatibilidadeDisponibilidade implements CriterioCompatibilidade {

    @Override
    public String nome() {
        return "Disponibilidade";
    }

    @Override
    public AvaliacaoCriterio avaliar(Projeto projeto, CandidatoProfissional candidato) {
        Disponibilidade disponibilidade = candidato.perfil().getDisponibilidade();
        String explicacao = "Disponibilidade: " + disponibilidade.rotulo().toLowerCase();
        return switch (disponibilidade) {
            case IMEDIATA -> AvaliacaoCriterio.atendido(nome(), explicacao);
            case EM_ATE_15_DIAS -> AvaliacaoCriterio.parcial(nome(), 0.7, explicacao);
            case EM_ATE_30_DIAS -> AvaliacaoCriterio.parcial(nome(), 0.4, explicacao);
            case INDISPONIVEL -> AvaliacaoCriterio.naoAtendido(nome(), explicacao);
        };
    }
}
