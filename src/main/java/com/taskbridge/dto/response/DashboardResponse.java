package com.taskbridge.dto.response;

import com.taskbridge.model.enums.TipoConta;

import java.util.List;

/**
 * Painel inicial. Os indicadores são uma lista (e não campos fixos) porque cliente e prestador
 * acompanham coisas diferentes; a interface só desenha o que recebe.
 */
public record DashboardResponse(
        TipoConta tipoConta,
        List<Indicador> indicadores,
        List<ProjetoResumoResponse> projetosAtivos,
        List<ContratacaoResumoResponse> contratacoesAtivas,
        List<ProjetoCompativelResponse> projetosCompativeis,
        SugestoesProjeto sugestoes) {

    public record Indicador(String chave, String rotulo, long valor, String link) {
    }

    /** Profissionais sugeridos para o projeto publicado mais recente do cliente. */
    public record SugestoesProjeto(Long projetoId, String projetoTitulo, List<CompatibilidadeResponse> profissionais) {
    }
}
