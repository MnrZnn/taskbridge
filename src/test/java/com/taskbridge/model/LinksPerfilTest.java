package com.taskbridge.model;

import com.taskbridge.model.UrlPublica;
import com.taskbridge.model.Usuario;
import com.taskbridge.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinksPerfilTest {

    @Test
    void completaHttpsEMantemOCaminho() {
        assertThat(UrlPublica.normalizar("linkedin.com/in/ana-ribeiro")).isEqualTo("https://linkedin.com/in/ana-ribeiro");
        assertThat(UrlPublica.normalizar("  HTTPS://github.com/ana  ")).isEqualTo("https://github.com/ana");
        assertThat(UrlPublica.normalizar("http://meu-portfolio.com.br/?aba=trabalhos")).isEqualTo("http://meu-portfolio.com.br/?aba=trabalhos");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(document.cookie)",
            "JaVaScRiPt:alert(1)",
            "data:text/html,<script>alert(1)</script>",
            "ftp://arquivos.exemplo.com",
            "https://banco.com@golpe.com/login",
            "https://localhost/admin",
            "https://exemplo.com/<script>",
            "nada"
    })
    void recusaEsquemasPerigososEUrlsEnganosas(String url) {
        assertThatThrownBy(() -> UrlPublica.normalizar(url)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void maximoDeCincoLinksSemRepetidos() {
        Usuario usuario = Fixtures.prestador();

        usuario.redefinirLinks(List.of("github.com/a", "https://github.com/a", "", "linkedin.com/in/a"));

        assertThat(usuario.getLinks()).containsExactly("https://github.com/a", "https://linkedin.com/in/a");
        assertThatThrownBy(() -> usuario.redefinirLinks(List.of("a.com", "b.com", "c.com", "d.com", "e.com", "f.com")))
                .hasMessageContaining("no máximo 5");
    }

    @Test
    void anonimizarApagaOsLinks() {
        Usuario usuario = Fixtures.prestador();
        usuario.redefinirLinks(List.of("github.com/a"));

        usuario.anonimizar("hash");

        assertThat(usuario.getLinks()).isEmpty();
    }
}
