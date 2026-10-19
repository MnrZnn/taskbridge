package com.taskbridge.dto.response;

import com.taskbridge.model.Categoria;

public record CategoriaResponse(Long id, String slug, String nome, String sigla, String descricao) {

    public static CategoriaResponse de(Categoria categoria) {
        if (categoria == null) {
            return null;
        }
        return new CategoriaResponse(categoria.getId(), categoria.getSlug(), categoria.getNome(),
                categoria.getSigla(), categoria.getDescricao());
    }
}
