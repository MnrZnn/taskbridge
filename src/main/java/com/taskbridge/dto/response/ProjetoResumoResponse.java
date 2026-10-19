package com.taskbridge.dto.response;

import com.taskbridge.model.enums.ProjetoStatus;
import com.taskbridge.model.FaixaValor;
import com.taskbridge.model.Projeto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProjetoResumoResponse(
        Long id,
        String titulo,
        String necessidade,
        CategoriaResponse categoria,
        ProjetoStatus status,
        String statusRotulo,
        BigDecimal orcamentoMinimo,
        BigDecimal orcamentoMaximo,
        Integer prazoDias,
        List<String> habilidades,
        Instant publicadoEm,
        Instant criadoEm) {

    public static ProjetoResumoResponse de(Projeto projeto) {
        FaixaValor orcamento = projeto.getOrcamento();
        return new ProjetoResumoResponse(
                projeto.getId(),
                projeto.getTitulo(),
                projeto.getNecessidade(),
                CategoriaResponse.de(projeto.getCategoria()),
                projeto.getStatus(),
                projeto.getStatus().rotulo(),
                orcamento == null ? null : orcamento.getMinimo(),
                orcamento == null ? null : orcamento.getMaximo(),
                projeto.getPrazoDias(),
                List.copyOf(projeto.getHabilidades()),
                projeto.getPublicadoEm(),
                projeto.getCriadoEm());
    }
}
