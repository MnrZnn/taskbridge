package com.taskbridge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExcluirContaRequest(
        @Size(max = 128) String senhaAtual,
        @NotBlank @Pattern(regexp = "EXCLUIR", message = "digite EXCLUIR para confirmar") String confirmacao) {

    @Override
    public String toString() {
        return "ExcluirContaRequest[***]";
    }
}
