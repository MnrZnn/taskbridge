package com.taskbridge.model;

import com.taskbridge.model.enums.UsuarioStatus;
import com.taskbridge.model.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.taskbridge.model.Fixtures.AGORA;
import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    @Test
    void bloqueiaTemporariamenteAposLimiteDeFalhas() {
        Usuario usuario = Fixtures.cliente();

        for (int i = 0; i < 4; i++) {
            usuario.registrarFalhaLogin(AGORA, 5, Duration.ofMinutes(15));
        }
        assertThat(usuario.estaBloqueado(AGORA)).isFalse();

        usuario.registrarFalhaLogin(AGORA, 5, Duration.ofMinutes(15));

        assertThat(usuario.estaBloqueado(AGORA.plus(Duration.ofMinutes(14)))).isTrue();
        assertThat(usuario.estaBloqueado(AGORA.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void anonimizarRemoveDadosPessoais() {
        Usuario usuario = Fixtures.prestador();
        usuario.atualizarDadosPessoais("Maria Souza", "Bio com detalhes", "Recife, PE");
        usuario.substituirFoto("foto.png");

        usuario.anonimizar("novo-hash");

        assertThat(usuario.getStatus()).isEqualTo(UsuarioStatus.EXCLUIDO);
        assertThat(usuario.getNomeCompleto()).isEqualTo("Usuário removido");
        assertThat(usuario.getEmail()).endsWith("@anonimizado.invalid").doesNotContain("x.test");
        assertThat(usuario.getBiografia()).isNull();
        assertThat(usuario.getLocalizacao()).isNull();
        assertThat(usuario.getFotoPerfil()).isNull();
        assertThat(usuario.getSenhaHash()).isEqualTo("novo-hash");
    }

    @Test
    void emailENomeDeUsuarioSaoNormalizados() {
        assertThat(Usuario.normalizarEmail("  Ana@Exemplo.COM ")).isEqualTo("ana@exemplo.com");
        assertThat(Usuario.normalizarNomeUsuario("Ana.Silva")).isEqualTo("ana.silva");
    }
}
