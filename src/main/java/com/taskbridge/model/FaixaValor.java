package com.taskbridge.model;

import com.taskbridge.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Intervalo de valores monetários. Usado como orçamento do projeto e como faixa de preço do prestador;
 * a comparação entre os dois é o núcleo do critério de orçamento do matching.
 */
@Embeddable
public class FaixaValor {

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minimo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal maximo;

    protected FaixaValor() {
    }

    public FaixaValor(BigDecimal minimo, BigDecimal maximo) {
        if (minimo == null || maximo == null) {
            throw new RegraNegocioException("Informe os valores mínimo e máximo.");
        }
        if (minimo.signum() < 0) {
            throw new RegraNegocioException("O valor mínimo não pode ser negativo.");
        }
        if (maximo.compareTo(minimo) < 0) {
            throw new RegraNegocioException("O valor máximo deve ser maior ou igual ao mínimo.");
        }
        this.minimo = minimo;
        this.maximo = maximo;
    }

    public static FaixaValor deOpcionais(BigDecimal minimo, BigDecimal maximo) {
        if (minimo == null && maximo == null) {
            return null;
        }
        return new FaixaValor(minimo, maximo);
    }

    public boolean sobrepoe(FaixaValor outra) {
        return minimo.compareTo(outra.maximo) <= 0 && outra.minimo.compareTo(maximo) <= 0;
    }

    public boolean contem(BigDecimal valor) {
        return valor.compareTo(minimo) >= 0 && valor.compareTo(maximo) <= 0;
    }

    public BigDecimal getMinimo() {
        return minimo;
    }

    public BigDecimal getMaximo() {
        return maximo;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FaixaValor outra
                && minimo.compareTo(outra.minimo) == 0
                && maximo.compareTo(outra.maximo) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minimo.stripTrailingZeros(), maximo.stripTrailingZeros());
    }
}
