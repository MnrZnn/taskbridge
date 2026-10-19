package com.taskbridge.model;

import com.taskbridge.model.enums.Disponibilidade;
import com.taskbridge.model.enums.ModalidadeTrabalho;

import java.util.Collection;

public record DadosProfissionais(
        String tituloProfissional,
        String descricao,
        int experienciaAnos,
        Disponibilidade disponibilidade,
        ModalidadeTrabalho modalidadeTrabalho,
        FaixaValor faixaPreco,
        Collection<String> habilidades) {
}
