package com.taskbridge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 160) String email,
        @NotBlank @Size(max = 128) String senha) {

    /** Records geram toString com todos os campos; este evita que a senha acabe num log por acidente. */
    @Override
    public String toString() {
        return "LoginRequest[senha=***]";
    }
}
