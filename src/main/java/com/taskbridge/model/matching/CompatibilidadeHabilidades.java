package com.taskbridge.model.matching;

import com.taskbridge.model.Projeto;

import java.util.List;
import java.util.Set;

public class CompatibilidadeHabilidades implements CriterioCompatibilidade {

    @Override
    public String nome() {
        return "Habilidades";
    }

    @Override
    public AvaliacaoCriterio avaliar(Projeto projeto, CandidatoProfissional candidato) {
        Set<String> exigidas = projeto.getHabilidades();
        if (exigidas.isEmpty()) {
            return AvaliacaoCriterio.atendido(nome(), "O projeto não exige habilidades específicas");
        }
        Set<String> doCandidato = candidato.perfil().getHabilidades();
        List<String> cobertas = exigidas.stream().filter(doCandidato::contains).toList();
        if (cobertas.isEmpty()) {
            return AvaliacaoCriterio.naoAtendido(nome(), "Nenhuma das habilidades pedidas no perfil");
        }
        double fator = (double) cobertas.size() / exigidas.size();
        String explicacao = "Domina %d de %d habilidades pedidas (%s)"
                .formatted(cobertas.size(), exigidas.size(), String.join(", ", cobertas));
        return AvaliacaoCriterio.parcial(nome(), fator, explicacao);
    }
}
