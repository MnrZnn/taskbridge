package com.taskbridge.dto.response;

import com.taskbridge.model.enums.ContratacaoStatus;
import com.taskbridge.model.enums.EntregaStatus;
import com.taskbridge.model.enums.TipoRequisito;
import com.taskbridge.model.Contratacao;
import com.taskbridge.model.Entrega;
import com.taskbridge.dto.response.ProjetoResponse.RequisitoResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ContratacaoResponse(
        Long id,
        Long projetoId,
        String projetoTitulo,
        UsuarioResumoResponse cliente,
        UsuarioResumoResponse prestador,
        BigDecimal valor,
        int prazoDias,
        ContratacaoStatus status,
        String statusRotulo,
        Instant criadaEm,
        Instant iniciadaEm,
        Instant concluidaEm,
        List<EntregaResponse> entregas,
        List<RequisitoResponse> criteriosAceite,
        Papel papel,
        Long pagamentoEmAbertoId) {

    public enum Papel { CLIENTE, PRESTADOR }

    public static ContratacaoResponse de(Contratacao contratacao, Long visitanteId, Long pagamentoEmAbertoId) {
        return new ContratacaoResponse(
                contratacao.getId(),
                contratacao.getProjeto().getId(),
                contratacao.getProjeto().getTitulo(),
                UsuarioResumoResponse.de(contratacao.getCliente()),
                UsuarioResumoResponse.de(contratacao.getPrestador()),
                contratacao.getValor(),
                contratacao.getProposta().getPrazoDias(),
                contratacao.getStatus(),
                contratacao.getStatus().rotulo(),
                contratacao.getCriadaEm(),
                contratacao.getIniciadaEm(),
                contratacao.getConcluidaEm(),
                contratacao.getEntregas().stream().map(EntregaResponse::de).toList(),
                contratacao.getProjeto().requisitosDoTipo(TipoRequisito.CRITERIO_ACEITE).stream()
                        .map(RequisitoResponse::de).toList(),
                contratacao.ehCliente(visitanteId) ? Papel.CLIENTE : Papel.PRESTADOR,
                pagamentoEmAbertoId);
    }

    public record EntregaResponse(
            Long id,
            String titulo,
            int ordem,
            EntregaStatus status,
            String statusRotulo,
            String conteudoEnvio,
            String feedbackRevisao,
            Instant enviadaEm,
            Instant avaliadaEm) {

        public static EntregaResponse de(Entrega entrega) {
            return new EntregaResponse(
                    entrega.getId(),
                    entrega.getTitulo(),
                    entrega.getOrdem(),
                    entrega.getStatus(),
                    entrega.getStatus().rotulo(),
                    entrega.getConteudoEnvio(),
                    entrega.getFeedbackRevisao(),
                    entrega.getEnviadaEm(),
                    entrega.getAvaliadaEm());
        }
    }
}
