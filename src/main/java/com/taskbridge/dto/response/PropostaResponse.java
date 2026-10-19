package com.taskbridge.dto.response;

import com.taskbridge.model.enums.PropostaStatus;
import com.taskbridge.model.Proposta;

import java.math.BigDecimal;
import java.time.Instant;

public record PropostaResponse(
        Long id,
        Long projetoId,
        String projetoTitulo,
        UsuarioResumoResponse prestador,
        BigDecimal valor,
        int prazoDias,
        String mensagem,
        PropostaStatus status,
        String statusRotulo,
        Instant criadaEm,
        Instant respondidaEm) {

    public static PropostaResponse de(Proposta proposta) {
        return new PropostaResponse(
                proposta.getId(),
                proposta.getProjeto().getId(),
                proposta.getProjeto().getTitulo(),
                UsuarioResumoResponse.de(proposta.getPrestador()),
                proposta.getValor(),
                proposta.getPrazoDias(),
                proposta.getMensagem(),
                proposta.getStatus(),
                proposta.getStatus().rotulo(),
                proposta.getCriadaEm(),
                proposta.getRespondidaEm());
    }
}
