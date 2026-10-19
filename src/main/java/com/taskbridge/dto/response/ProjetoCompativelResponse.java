package com.taskbridge.dto.response;

import com.taskbridge.dto.response.CompatibilidadeResponse.CriterioResponse;

import java.util.List;

/** Projeto aberto sugerido a um prestador, com a explicação da compatibilidade. */
public record ProjetoCompativelResponse(
        ProjetoResumoResponse projeto,
        int pontuacao,
        List<CriterioResponse> criterios,
        boolean propostaEnviada) {
}
