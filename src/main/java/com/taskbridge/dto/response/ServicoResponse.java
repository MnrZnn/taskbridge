package com.taskbridge.dto.response;

import com.taskbridge.model.enums.ServicoStatus;
import com.taskbridge.model.Servico;

import java.math.BigDecimal;
import java.time.Instant;

public record ServicoResponse(
        Long id,
        String titulo,
        String descricao,
        BigDecimal precoBase,
        int prazoDias,
        ServicoStatus status,
        CategoriaResponse categoria,
        UsuarioResumoResponse prestador,
        Instant criadoEm) {

    public static ServicoResponse de(Servico servico) {
        return new ServicoResponse(
                servico.getId(),
                servico.getTitulo(),
                servico.getDescricao(),
                servico.getPrecoBase(),
                servico.getPrazoDias(),
                servico.getStatus(),
                CategoriaResponse.de(servico.getCategoria()),
                UsuarioResumoResponse.de(servico.getPrestador()),
                servico.getCriadoEm());
    }
}
