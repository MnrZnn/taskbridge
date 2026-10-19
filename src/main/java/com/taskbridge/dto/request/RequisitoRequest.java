package com.taskbridge.dto.request;

import com.taskbridge.model.enums.TipoRequisito;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RequisitoRequest(
        @NotNull TipoRequisito tipo,
        @NotBlank @Size(max = 500) String descricao) {
}
