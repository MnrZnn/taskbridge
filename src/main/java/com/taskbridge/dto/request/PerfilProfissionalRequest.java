package com.taskbridge.dto.request;

import com.taskbridge.model.enums.Disponibilidade;
import com.taskbridge.model.enums.ModalidadeTrabalho;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record PerfilProfissionalRequest(
        @NotBlank @Size(min = 3, max = 100) String tituloProfissional,
        @Size(max = 2000) String descricao,
        @NotNull @Min(0) @Max(60) Integer experienciaAnos,
        @NotNull Disponibilidade disponibilidade,
        @NotNull ModalidadeTrabalho modalidadeTrabalho,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal faixaPrecoMinima,
        @NotNull @DecimalMin("1.00") @Digits(integer = 10, fraction = 2) BigDecimal faixaPrecoMaxima,
        @Size(max = 15) List<@NotBlank @Size(max = 40) String> habilidades) {
}
