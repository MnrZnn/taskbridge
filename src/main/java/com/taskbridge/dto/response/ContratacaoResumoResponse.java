package com.taskbridge.dto.response;

import com.taskbridge.model.enums.ContratacaoStatus;
import com.taskbridge.model.Contratacao;

import java.math.BigDecimal;
import java.time.Instant;

public record ContratacaoResumoResponse(
        Long id,
        Long projetoId,
        String projetoTitulo,
        UsuarioResumoResponse cliente,
        UsuarioResumoResponse prestador,
        BigDecimal valor,
        ContratacaoStatus status,
        String statusRotulo,
        Instant criadaEm) {

    public static ContratacaoResumoResponse de(Contratacao contratacao) {
        return new ContratacaoResumoResponse(
                contratacao.getId(),
                contratacao.getProjeto().getId(),
                contratacao.getProjeto().getTitulo(),
                UsuarioResumoResponse.de(contratacao.getCliente()),
                UsuarioResumoResponse.de(contratacao.getPrestador()),
                contratacao.getValor(),
                contratacao.getStatus(),
                contratacao.getStatus().rotulo(),
                contratacao.getCriadaEm());
    }
}
