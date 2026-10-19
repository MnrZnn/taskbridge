package com.taskbridge.dto.request;

import com.taskbridge.model.enums.TipoConta;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** O e-mail não vem daqui: ele é o verificado pelo provedor, guardado na sessão. */
public record ConcluirCadastroSocialRequest(
        @NotBlank @Size(min = 3, max = 120) String nomeCompleto,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_.]{3,30}$",
                message = "use de 3 a 30 letras, números, ponto ou sublinhado") String nomeUsuario,
        @NotNull TipoConta tipoConta,
        @AssertTrue(message = "é necessário aceitar os termos de uso e a política de privacidade") boolean aceiteTermos) {
}
