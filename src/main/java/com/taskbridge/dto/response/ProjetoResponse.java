package com.taskbridge.dto.response;

import com.taskbridge.model.enums.ProjetoStatus;
import com.taskbridge.model.enums.TipoRequisito;
import com.taskbridge.model.FaixaValor;
import com.taskbridge.model.Projeto;
import com.taskbridge.model.Requisito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProjetoResponse(
        Long id,
        String titulo,
        String necessidade,
        String objetivo,
        String descricao,
        CategoriaResponse categoria,
        BigDecimal orcamentoMinimo,
        BigDecimal orcamentoMaximo,
        Integer prazoDias,
        ProjetoStatus status,
        String statusRotulo,
        List<String> habilidades,
        List<RequisitoResponse> requisitos,
        UsuarioResumoResponse cliente,
        Instant publicadoEm,
        Instant criadoEm,
        boolean proprietario,
        boolean aceitaPropostas,
        List<String> pendenciasPublicacao) {

    /** As pendências de publicação só interessam (e só são enviadas) ao dono do rascunho. */
    public static ProjetoResponse de(Projeto projeto, Long visitanteId) {
        boolean dono = visitanteId != null && projeto.pertenceA(visitanteId);
        boolean rascunho = projeto.getStatus() == ProjetoStatus.DRAFT;
        FaixaValor orcamento = projeto.getOrcamento();
        return new ProjetoResponse(
                projeto.getId(),
                projeto.getTitulo(),
                projeto.getNecessidade(),
                projeto.getObjetivo(),
                projeto.getDescricao(),
                CategoriaResponse.de(projeto.getCategoria()),
                orcamento == null ? null : orcamento.getMinimo(),
                orcamento == null ? null : orcamento.getMaximo(),
                projeto.getPrazoDias(),
                projeto.getStatus(),
                projeto.getStatus().rotulo(),
                List.copyOf(projeto.getHabilidades()),
                projeto.getRequisitos().stream().map(RequisitoResponse::de).toList(),
                UsuarioResumoResponse.de(projeto.getCliente()),
                projeto.getPublicadoEm(),
                projeto.getCriadoEm(),
                dono,
                projeto.getStatus().aceitaPropostas(),
                dono && rascunho ? projeto.pendenciasParaPublicacao() : List.of());
    }

    public record RequisitoResponse(TipoRequisito tipo, String descricao) {

        public static RequisitoResponse de(Requisito requisito) {
            return new RequisitoResponse(requisito.getTipo(), requisito.getDescricao());
        }
    }
}
