package com.taskbridge.security;

import com.taskbridge.config.RateLimitProperties.Limite;
import com.taskbridge.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SegurancaUnitariaTest {

    @Test
    void limitadorBloqueiaDepoisDaCapacidadeEPorChave() {
        LimitadorRequisicoes limitador = new LimitadorRequisicoes(100, Duration.ofMinutes(5));
        Limite limite = new Limite(3, Duration.ofMinutes(1));

        for (int i = 0; i < 3; i++) {
            assertThat(limitador.consumir("LOGIN:10.0.0.1", limite).isConsumed()).isTrue();
        }
        assertThat(limitador.consumir("LOGIN:10.0.0.1", limite).isConsumed()).isFalse();
        assertThat(limitador.consumir("LOGIN:10.0.0.2", limite).isConsumed()).isTrue();
    }

    @Test
    void politicaDeSenha() {
        assertThatThrownBy(() -> PoliticaSenha.validar("curta1")).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> PoliticaSenha.validar("somenteletras")).hasMessageContaining("letras e números");
        assertThatThrownBy(() -> PoliticaSenha.validar("á".repeat(40) + "1")).hasMessageContaining("longa");
        assertThatCode(() -> PoliticaSenha.validar("senhaboa123")).doesNotThrowAnyException();
    }

    @Test
    void principalNaoGuardaSenhaDepoisDaAutenticacao() {
        UsuarioAutenticado principal = new UsuarioAutenticado(com.taskbridge.model.Fixtures.cliente(), false);

        principal.eraseCredentials();

        assertThat(principal.getPassword()).isNull();
        assertThat(principal.toString()).doesNotContain("hash");
    }
}
