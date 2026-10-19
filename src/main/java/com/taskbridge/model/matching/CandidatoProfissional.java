package com.taskbridge.model.matching;

import com.taskbridge.model.Categoria;
import com.taskbridge.model.PerfilProfissional;
import com.taskbridge.model.Servico;

import java.util.List;
import java.util.OptionalInt;

/** Perfil profissional + serviços ativos: o que os critérios precisam saber sobre o prestador. */
public record CandidatoProfissional(PerfilProfissional perfil, List<Servico> servicosAtivos) {

    public CandidatoProfissional {
        servicosAtivos = List.copyOf(servicosAtivos);
    }

    public boolean atendeCategoria(Categoria categoria) {
        return servicosAtivos.stream().anyMatch(servico -> servico.getCategoria().getId().equals(categoria.getId()));
    }

    /** Menor prazo entre os serviços da categoria; se não houver, o menor entre todos os serviços. */
    public OptionalInt prazoDeReferencia(Categoria categoria) {
        OptionalInt naCategoria = servicosAtivos.stream()
                .filter(servico -> categoria != null && servico.getCategoria().getId().equals(categoria.getId()))
                .mapToInt(Servico::getPrazoDias)
                .min();
        return naCategoria.isPresent() ? naCategoria : servicosAtivos.stream().mapToInt(Servico::getPrazoDias).min();
    }
}
