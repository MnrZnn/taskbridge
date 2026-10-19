package com.taskbridge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarEntregaRequest(@NotBlank @Size(max = 2000) String conteudo) {
}
