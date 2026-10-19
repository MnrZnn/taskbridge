package com.taskbridge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitarRevisaoRequest(@NotBlank @Size(max = 1000) String feedback) {
}
