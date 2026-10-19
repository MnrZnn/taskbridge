package com.taskbridge.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Serve para criar e para salvar o rascunho a cada etapa do assistente — por isso quase tudo é
 * opcional aqui. O que é exigido para publicar é regra de domínio (Projeto.pendenciasParaPublicacao).
 * Não há campo de status, cliente ou id: esses valores o cliente não controla.
 */
public record ProjetoRequest(
        @NotBlank @Size(min = 5, max = 150) String titulo,
        @NotBlank @Size(min = 10, max = 2000) String necessidade,
        Long categoriaId,
        @Size(max = 500) String objetivo,
        @Size(max = 4000) String descricao,
        @DecimalMin("1.00") @Digits(integer = 10, fraction = 2) BigDecimal orcamentoMinimo,
        @DecimalMin("1.00") @Digits(integer = 10, fraction = 2) BigDecimal orcamentoMaximo,
        @Min(1) @Max(365) Integer prazoDias,
        @Size(max = 15) List<@NotBlank @Size(max = 40) String> habilidades,
        @Size(max = 30) List<@Valid RequisitoRequest> requisitos) {
}
