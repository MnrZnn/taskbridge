package com.taskbridge.dto.response;

import com.taskbridge.model.enums.Disponibilidade;
import com.taskbridge.model.enums.ModalidadeTrabalho;
import com.taskbridge.model.enums.PerfilStatus;
import com.taskbridge.model.PerfilProfissional;

import java.math.BigDecimal;
import java.util.List;

public record PerfilProfissionalResponse(
        String tituloProfissional,
        String descricao,
        int experienciaAnos,
        Disponibilidade disponibilidade,
        String disponibilidadeRotulo,
        ModalidadeTrabalho modalidadeTrabalho,
        BigDecimal faixaPrecoMinima,
        BigDecimal faixaPrecoMaxima,
        PerfilStatus status,
        List<String> habilidades) {

    public static PerfilProfissionalResponse de(PerfilProfissional perfil) {
        return new PerfilProfissionalResponse(
                perfil.getTituloProfissional(),
                perfil.getDescricao(),
                perfil.getExperienciaAnos(),
                perfil.getDisponibilidade(),
                perfil.getDisponibilidade().rotulo(),
                perfil.getModalidadeTrabalho(),
                perfil.getFaixaPreco().getMinimo(),
                perfil.getFaixaPreco().getMaximo(),
                perfil.getStatus(),
                List.copyOf(perfil.getHabilidades()));
    }
}
