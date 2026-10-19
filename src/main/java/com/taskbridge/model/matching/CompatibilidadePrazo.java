package com.taskbridge.model.matching;

import com.taskbridge.model.Projeto;

import java.util.OptionalInt;

/**
 * Estima em quantos dias o prestador entregaria: dias até ficar disponível + menor prazo
 * dos serviços dele (preferindo os da categoria do projeto).
 */
public class CompatibilidadePrazo implements CriterioCompatibilidade {

    private static final double TOLERANCIA = 1.25;

    @Override
    public String nome() {
        return "Prazo";
    }

    @Override
    public AvaliacaoCriterio avaliar(Projeto projeto, CandidatoProfissional candidato) {
        Integer prazoProjeto = projeto.getPrazoDias();
        if (prazoProjeto == null) {
            return AvaliacaoCriterio.naoAtendido(nome(), "Projeto sem prazo definido");
        }
        OptionalInt prazoServico = candidato.prazoDeReferencia(projeto.getCategoria());
        if (prazoServico.isEmpty()) {
            return AvaliacaoCriterio.parcial(nome(), 0.5, "Sem serviço cadastrado para estimar o prazo");
        }
        int estimativa = candidato.perfil().getDisponibilidade().diasAteInicio() + prazoServico.getAsInt();
        String detalhe = "Entrega estimada em %d dias (prazo do projeto: %d)".formatted(estimativa, prazoProjeto);
        if (estimativa <= prazoProjeto) {
            return AvaliacaoCriterio.atendido(nome(), detalhe);
        }
        if (estimativa <= prazoProjeto * TOLERANCIA) {
            return AvaliacaoCriterio.parcial(nome(), 0.5, detalhe);
        }
        return AvaliacaoCriterio.naoAtendido(nome(), detalhe);
    }
}
