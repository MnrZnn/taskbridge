package com.taskbridge.integracao;

import com.taskbridge.model.enums.UsuarioStatus;
import com.taskbridge.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ValidacaoEntradaIT extends TesteIntegracao {

    @Test
    void camposQueOClienteNaoControlaSaoRecusados() throws Exception {
        Usuario cliente = novoCliente();
        for (String campoProibido : new String[]{"status", "clienteId", "id", "publicadoEm"}) {
            Map<String, Object> corpo = new HashMap<>(projetoCompleto("Projeto com campo extra"));
            corpo.put(campoProibido, "PUBLISHED");

            mvc.perform(postJson("/api/projetos", corpo).with(como(cliente)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Campo não permitido: " + campoProibido));
        }
    }

    @Test
    void usuarioNaoAlteraProprioIdNemStatus() throws Exception {
        Usuario usuario = novoCliente();

        mvc.perform(put("/api/me").with(csrf()).with(como(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nomeCompleto", "Novo Nome", "id", 1))))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/me").with(csrf()).with(como(usuario)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nomeCompleto", "Novo Nome", "status", "EXCLUIDO", "tipoConta", "PRESTADOR"))))
                .andExpect(status().isBadRequest());

        Usuario recarregado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(recarregado.getStatus()).isEqualTo(UsuarioStatus.ATIVO);
        assertThat(recarregado.getNomeCompleto()).isEqualTo(usuario.getNomeCompleto());
    }

    @Test
    void injecaoDeSqlNaBuscaNaoAlteraAConsulta() throws Exception {
        criarProjetoPublicado(novoCliente());

        for (String ataque : new String[]{"' OR '1'='1", "x' UNION SELECT senha_hash FROM usuario --", "%", "_"}) {
            mvc.perform(get("/api/projetos").param("texto", ataque))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElementos").value(0));
        }
        mvc.perform(get("/api/projetos").param("categoriaId", "1 OR 1=1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buscaNormalContinuaFuncionando() throws Exception {
        criarProjetoPublicado(novoCliente());

        mvc.perform(get("/api/projetos").param("texto", "formulário de contato"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void paginacaoTemTetoEDadosInvalidosSaoRecusados() throws Exception {
        mvc.perform(get("/api/projetos").param("size", "500")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/projetos").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/projetos").param("size", "50")).andExpect(status().isOk());
    }

    @Test
    void beanValidationDevolveOsCamposComErro() throws Exception {
        mvc.perform(postJson("/api/projetos", Map.of("titulo", "", "necessidade", "curta")).with(como(novoCliente())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.titulo").exists())
                .andExpect(jsonPath("$.campos.necessidade").exists());
    }

    @Test
    void publicarProjetoIncompletoDevolve422ComPendencias() throws Exception {
        Usuario cliente = novoCliente();
        var resultado = mvc.perform(postJson("/api/projetos", Map.of("titulo", "Só o título", "necessidade", "Uma necessidade qualquer"))
                .with(como(cliente))).andExpect(status().isCreated()).andReturn();
        long id = corpo(resultado).get("id").asLong();

        mvc.perform(post("/api/projetos/" + id + "/publicar").with(csrf()).with(como(cliente)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detalhes").isArray());
    }

    @Test
    void corpoGrandeDemaisEhRecusadoAntesDeSerLido() throws Exception {
        String enorme = "x".repeat(70 * 1024);

        mvc.perform(post("/api/projetos").with(csrf()).with(como(novoCliente())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"" + enorme + "\"}"))
                .andExpect(status().is(413));
    }

    @Test
    void uploadInvalidoEhRecusado() throws Exception {
        MockMultipartFile falso = new MockMultipartFile("arquivo", "foto.png", "image/png", "<?php system($_GET['c']); ?>".getBytes());

        mvc.perform(multipart("/api/me/foto").file(falso).with(csrf()).with(como(novoCliente())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O conteúdo do arquivo não é uma imagem JPG ou PNG."));
    }

    @Test
    void errosNaoExpoemDetalhesInternos() throws Exception {
        mvc.perform(post("/api/projetos").with(csrf()).with(como(novoCliente())).contentType(MediaType.APPLICATION_JSON)
                        .content("{json quebrado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido."))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }
}
