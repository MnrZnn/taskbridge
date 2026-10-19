package com.taskbridge.integracao;

import com.taskbridge.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controle de acesso (OWASP A01 / IDOR / BOLA): conhecer o id não dá acesso ao recurso.
 * Recurso privado de outra pessoa → 404 (não confirma que existe). Recurso visível, ação de outro → 403.
 */
class AutorizacaoIT extends TesteIntegracao {

    @Test
    void usuarioNaoAcessaRascunhoDeOutroUsuario() throws Exception {
        long rascunhoDeB = criarRascunho(novoCliente());

        mvc.perform(get("/api/projetos/" + rascunhoDeB).with(como(novoCliente()))).andExpect(status().isNotFound());
        mvc.perform(get("/api/projetos/" + rascunhoDeB)).andExpect(status().isNotFound());
    }

    @Test
    void usuarioNaoAlteraProjetoDeOutroCliente() throws Exception {
        long projetoDeB = criarProjetoPublicado(novoCliente());
        Usuario clienteA = novoCliente();

        mvc.perform(put("/api/projetos/" + projetoDeB).with(csrf()).with(como(clienteA))
                        .contentType("application/json").content(json(projetoCompleto("Invadido"))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/projetos/" + projetoDeB + "/cancelar").with(csrf()).with(como(clienteA)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/projetos/" + projetoDeB + "/propostas").with(como(clienteA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioNaoAlteraServicoDeOutroPrestador() throws Exception {
        Usuario prestadorB = novoPrestador();
        MvcResult criado = mvc.perform(postJson("/api/servicos", Map.of("categoriaId", 1, "titulo", "Site institucional",
                        "descricao", "Desenvolvimento de site institucional completo.", "precoBase", 1500, "prazoDias", 15))
                        .with(como(prestadorB)))
                .andExpect(status().isCreated()).andReturn();
        long servicoDeB = corpo(criado).get("id").asLong();
        Usuario prestadorA = novoPrestador();

        mvc.perform(put("/api/servicos/" + servicoDeB).with(csrf()).with(como(prestadorA)).contentType("application/json")
                        .content(json(Map.of("categoriaId", 1, "titulo", "Tomado", "descricao", "Descrição alterada por outra pessoa.",
                                "precoBase", 1, "prazoDias", 1))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/servicos/" + servicoDeB).with(csrf()).with(como(prestadorA)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/servicos/" + servicoDeB + "/pausar").with(csrf()).with(como(prestadorA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void prestadorNaoAceitaProposta() throws Exception {
        long projeto = criarProjetoPublicado(novoCliente());
        Usuario prestador = novoPrestador();
        long proposta = enviarProposta(prestador, projeto);

        mvc.perform(post("/api/propostas/" + proposta + "/aceitar").with(csrf()).with(como(prestador)))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteNaoEnviaProposta() throws Exception {
        long projeto = criarProjetoPublicado(novoCliente());

        mvc.perform(postJson("/api/projetos/" + projeto + "/propostas",
                        Map.of("valor", 100, "prazoDias", 5, "mensagem", "Proposta enviada por um cliente."))
                        .with(como(novoCliente())))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteNaoAceitaPropostaDeProjetoAlheio() throws Exception {
        long projeto = criarProjetoPublicado(novoCliente());
        long proposta = enviarProposta(novoPrestador(), projeto);

        mvc.perform(post("/api/propostas/" + proposta + "/aceitar").with(csrf()).with(como(novoCliente())))
                .andExpect(status().isNotFound());
    }

    @Test
    void contratacaoEPagamentoSaoInvisiveisParaTerceiros() throws Exception {
        long contratacao = criarContratacao(novoCliente(), novoPrestador());
        Usuario terceiro = novoCliente();

        mvc.perform(get("/api/contratacoes/" + contratacao).with(como(terceiro))).andExpect(status().isNotFound());
        mvc.perform(post("/api/contratacoes/" + contratacao + "/pagamentos").with(csrf()).with(como(terceiro)))
                .andExpect(status().isNotFound());
    }

    @Test
    void propostaEhPrivadaEntreAsPartes() throws Exception {
        long projeto = criarProjetoPublicado(novoCliente());
        long proposta = enviarProposta(novoPrestador(), projeto);

        mvc.perform(post("/api/propostas/" + proposta + "/cancelar").with(csrf()).with(como(novoPrestador())))
                .andExpect(status().isNotFound());
    }
}
