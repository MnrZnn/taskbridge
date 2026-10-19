package com.taskbridge.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Paginação obrigatória com teto de 50 itens. A ordenação é sempre decidida pelo servidor:
 * aceitar "sort=qualquerCampo" permitiria ordenar por colunas internas e inferir valores ocultos.
 */
public record PaginaRequest(@Min(0) @Max(10_000) Integer page, @Min(1) @Max(50) Integer size) {

    private static final int TAMANHO_PADRAO = 20;

    public Pageable ordenadoPor(Sort ordenacao) {
        int pagina = page == null ? 0 : page;
        int tamanho = size == null ? TAMANHO_PADRAO : size;
        return PageRequest.of(pagina, tamanho, ordenacao);
    }
}
