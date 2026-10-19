package com.taskbridge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequest(
        @Size(max = 128) String senhaAtual,
        @NotBlank @Size(min = 10, max = 64) String novaSenha) {

    @Override
    public String toString() {
        return "AlterarSenhaRequest[***]";
    }
}
