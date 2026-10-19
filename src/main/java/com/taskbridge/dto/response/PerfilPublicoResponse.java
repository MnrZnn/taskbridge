package com.taskbridge.dto.response;

import com.taskbridge.model.enums.TipoConta;

import java.time.Instant;
import java.util.List;

/** Página pública de um usuário. Sem e-mail e sem dados de contratações em andamento. */
public record PerfilPublicoResponse(
        String nomeUsuario,
        String nomeCompleto,
        TipoConta tipoConta,
        String biografia,
        String localizacao,
        String fotoUrl,
        List<String> links,
        Instant membroDesde,
        PerfilProfissionalResponse perfilProfissional,
        List<ServicoResponse> servicos,
        Estatisticas estatisticas,
        List<ItemHistorico> historico) {

    public record Estatisticas(long projetosConcluidos, long servicosAtivos, long emAndamento) {
    }

    public record ItemHistorico(String titulo, String categoria, Instant concluidoEm) {
    }
}
