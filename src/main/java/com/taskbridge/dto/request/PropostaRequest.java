package com.taskbridge.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PropostaRequest(
        @NotNull @DecimalMin("1.00") @Digits(integer = 10, fraction = 2) BigDecimal valor,
        @NotNull @Min(1) @Max(365) Integer prazoDias,
        @NotBlank @Size(min = 20, max = 2000) String mensagem) {
}
