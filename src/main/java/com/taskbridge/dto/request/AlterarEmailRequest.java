package com.taskbridge.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarEmailRequest(
        @NotBlank @Email @Size(max = 160) String novoEmail,
        @NotBlank @Size(max = 128) String senhaAtual) {

    @Override
    public String toString() {
        return "AlterarEmailRequest[senhaAtual=***]";
    }
}
