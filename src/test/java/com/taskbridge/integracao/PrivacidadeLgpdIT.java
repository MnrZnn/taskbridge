package com.taskbridge.integracao;

import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.model.enums.UsuarioStatus;
import com.taskbridge.model.Usuario;
import com.taskbridge.repository.RegistroAuditoriaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PrivacidadeLgpdIT extends TesteIntegracao {

    @Autowired
    private RegistroAuditoriaRepository auditoria;

    @Test
    void exportacaoTrazOsDadosDoTitularSemSegredos() throws Exception {
        Usuario cliente = novoCliente();
        criarRascunho(cliente);

        mvc.perform(get("/api/me/dados").with(como(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conta.email").value(cliente.getEmail()))
                .andExpect(jsonPath("$.termosVersao").value("teste"))
                .andExpect(jsonPath("$.projetos.length()").value(1))
                .andExpect(jsonPath("$.conta.senhaHash").doesNotExist());
    }

    @Test
    void linksDoPerfilSaoValidadosEAparecemNoPerfilPublico() throws Exception {
        Usuario prestador = novoPrestador();

        mvc.perform(put("/api/me/links").with(csrf()).with(como(prestador)).contentType("application/json")
                        .content(json(Map.of("links", java.util.List.of("javascript:alert(1)")))))
                .andExpect(status().is(422));
        mvc.perform(put("/api/me/links").with(csrf()).with(como(prestador)).contentType("application/json")
                        .content(json(Map.of("links", java.util.List.of("linkedin.com/in/fulano", "https://github.com/fulano")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[0]").value("https://linkedin.com/in/fulano"));

        mvc.perform(get("/api/usuarios/" + prestador.getNomeUsuario()))
                .andExpect(jsonPath("$.links.length()").value(2))
                .andExpect(jsonPath("$.links[1]").value("https://github.com/fulano"));
    }

    @Test
    void perfilPublicoNaoExpoeEmail() throws Exception {
        Usuario prestador = novoPrestador();

        mvc.perform(get("/api/usuarios/" + prestador.getNomeUsuario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.nomeUsuario").value(prestador.getNomeUsuario()));
    }

    @Test
    void naoExcluiContaComContratacaoEmAndamento() throws Exception {
        Usuario cliente = novoCliente();
        criarContratacao(cliente, novoPrestador());

        mvc.perform(postJson("/api/me/exclusao", Map.of("senhaAtual", SENHA, "confirmacao", "EXCLUIR")).with(como(cliente)))
                .andExpect(status().isConflict());
    }

    @Test
    void exclusaoExigeSenhaEConfirmacao() throws Exception {
        Usuario cliente = novoCliente();

        mvc.perform(postJson("/api/me/exclusao", Map.of("senhaAtual", "senhaErrada99", "confirmacao", "EXCLUIR")).with(como(cliente)))
                .andExpect(status().is(422));
        mvc.perform(postJson("/api/me/exclusao", Map.of("senhaAtual", SENHA, "confirmacao", "sim")).with(como(cliente)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exclusaoAnonimizaECancelaOQueEstavaAberto() throws Exception {
        Usuario cliente = novoCliente();
        long projetoAberto = criarProjetoPublicado(cliente);
        Usuario prestador = novoPrestador();
        enviarProposta(prestador, projetoAberto);

        mvc.perform(postJson("/api/me/exclusao", Map.of("senhaAtual", SENHA, "confirmacao", "EXCLUIR")).with(como(cliente)))
                .andExpect(status().isNoContent());

        Usuario excluido = usuarioRepository.findById(cliente.getId()).orElseThrow();
        assertThat(excluido.getStatus()).isEqualTo(UsuarioStatus.EXCLUIDO);
        assertThat(excluido.getEmail()).doesNotContain(cliente.getEmail());
        assertThat(excluido.getNomeCompleto()).isEqualTo("Usuário removido");

        mvc.perform(get("/api/usuarios/" + cliente.getNomeUsuario())).andExpect(status().isNotFound());
        mvc.perform(postJson("/api/auth/login", Map.of("email", cliente.getEmail(), "senha", SENHA)).with(ipNovo()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/propostas/enviadas").with(como(prestador)))
                .andExpect(jsonPath("$.conteudo[0].status").value("REJECTED"));
        assertThat(auditoria.findByUsuarioId(cliente.getId(), Pageable.unpaged()))
                .extracting("evento").contains(EventoAuditoria.CONTA_EXCLUIDA);
    }

    @Test
    void eventosDeSegurancaSaoDoProprioUsuario() throws Exception {
        Usuario usuario = novoCliente();
        mvc.perform(postJson("/api/auth/login", Map.of("email", usuario.getEmail(), "senha", SENHA)).with(ipNovo()));

        mvc.perform(get("/api/me/eventos-seguranca").with(como(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].evento").value("LOGIN_SUCESSO"));
        mvc.perform(get("/api/me/eventos-seguranca").with(como(novoCliente())))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void logoutAposExclusaoInvalidaSessao() throws Exception {
        Usuario cliente = novoCliente();

        mvc.perform(post("/api/me/exclusao").with(csrf()).with(como(cliente)).contentType("application/json")
                        .content(json(Map.of("senhaAtual", SENHA, "confirmacao", "EXCLUIR"))))
                .andExpect(status().isNoContent())
                .andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull());
    }
}
