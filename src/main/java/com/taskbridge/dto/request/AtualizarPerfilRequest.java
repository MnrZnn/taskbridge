package com.taskbridge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarPerfilRequest(
        @NotBlank @Size(min = 3, max = 120) String nomeCompleto,
        @Size(max = 1000) String biografia,
        @Size(max = 80) String localizacao) {
}
