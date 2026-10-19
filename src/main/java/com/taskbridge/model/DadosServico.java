package com.taskbridge.model;

import java.math.BigDecimal;

public record DadosServico(Categoria categoria, String titulo, String descricao, BigDecimal precoBase, int prazoDias) {
}
