package com.taskbridge.integracao;

import com.taskbridge.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** NECESSIDADE → ESCOPO → MATCHING → PROPOSTA → CONTRATAÇÃO → PIX → ENTREGAS → ACEITE → CONCLUSÃO, pela API. */
class FluxoContratacaoIT extends TesteIntegracao {

    @Test
    void fluxoCompletoDaNecessidadeAConclusao() throws Exception {
        Usuario cliente = novoCliente();
        Usuario prestador = novoPrestador();
        Usuario concorrente = novoPrestador();
        mvc.perform(put("/api/me/perfil-profissional").with(csrf()).with(como(prestador)).contentType("application/json")
                        .content(json(Map.of("tituloProfissional", "Desenvolvedor Web", "experienciaAnos", 6,
                                "disponibilidade", "IMEDIATA", "modalidadeTrabalho", "REMOTO",
                                "faixaPrecoMinima", 1000, "faixaPrecoMaxima", 5000, "habilidades", java.util.List.of("HTML", "css")))))
                .andExpect(status().isOk());

        long projetoId = criarProjetoPublicado(cliente);

        // Matching: o prestador aparece com a explicação de cada critério.
        mvc.perform(get("/api/projetos/" + projetoId + "/profissionais-compativeis").with(como(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.profissional.nomeUsuario == '" + prestador.getNomeUsuario() + "')].criterios[0].explicacao").exists());

        long propostaAceita = enviarProposta(prestador, projetoId);
        long propostaPerdedora = enviarProposta(concorrente, projetoId);

        MvcResult aceite = mvc.perform(post("/api/propostas/" + propostaAceita + "/aceitar").with(csrf()).with(como(cliente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT"))
                .andReturn();
        long contratacaoId = corpo(aceite).get("id").asLong();
        long entregaId = corpo(aceite).get("entregas").get(0).get("id").asLong();

        mvc.perform(get("/api/propostas/enviadas").with(como(concorrente)))
                .andExpect(jsonPath("$.conteudo[0].id").value(propostaPerdedora))
                .andExpect(jsonPath("$.conteudo[0].status").value("REJECTED"));
        mvc.perform(post("/api/propostas/" + propostaPerdedora + "/aceitar").with(csrf()).with(como(cliente)))
                .andExpect(status().isConflict());

        // Entregas bloqueadas até o pagamento.
        mvc.perform(postJson("/api/contratacoes/" + contratacaoId + "/entregas/" + entregaId + "/enviar", Map.of("conteudo", "cedo demais"))
                        .with(como(prestador)))
                .andExpect(status().is(422));

        MvcResult cobranca = mvc.perform(post("/api/contratacoes/" + contratacaoId + "/pagamentos").with(csrf()).with(como(cliente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.qrCode").value(org.hamcrest.Matchers.startsWith("data:image/png;base64,")))
                .andExpect(jsonPath("$.ambienteTeste").value(true))
                .andReturn();
        long pagamentoId = corpo(cobranca).get("id").asLong();

        mvc.perform(post("/api/debug/pagamentos/" + pagamentoId + "/simular-pagamento").with(csrf()).with(como(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.qrCode").doesNotExist());

        mvc.perform(postJson("/api/contratacoes/" + contratacaoId + "/entregas/" + entregaId + "/enviar", Map.of("conteudo", "Site no ar"))
                        .with(como(prestador)))
                .andExpect(jsonPath("$.entregas[0].status").value("SUBMITTED"));
        mvc.perform(postJson("/api/contratacoes/" + contratacaoId + "/entregas/" + entregaId + "/solicitar-revisao", Map.of("feedback", "Ajustar o rodapé"))
                        .with(como(cliente)))
                .andExpect(jsonPath("$.entregas[0].status").value("REVISION_REQUIRED"));
        mvc.perform(postJson("/api/contratacoes/" + contratacaoId + "/entregas/" + entregaId + "/enviar", Map.of("conteudo", "Rodapé ajustado"))
                        .with(como(prestador)))
                .andExpect(jsonPath("$.entregas[0].status").value("SUBMITTED"));

        MvcResult conclusao = mvc.perform(post("/api/contratacoes/" + contratacaoId + "/entregas/" + entregaId + "/aprovar")
                        .with(csrf()).with(como(cliente)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode contratacao = corpo(conclusao);
        assertThat(contratacao.get("status").asString()).isEqualTo("COMPLETED");
        assertThat(contratacao.get("concluidaEm").isNull()).isFalse();

        mvc.perform(get("/api/projetos/" + projetoId)).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/usuarios/" + prestador.getNomeUsuario()))
                .andExpect(jsonPath("$.estatisticas.projetosConcluidos").value(1))
                .andExpect(jsonPath("$.historico[0].titulo").value("Projeto de teste"));
        mvc.perform(get("/api/notificacoes").with(como(prestador)))
                .andExpect(jsonPath("$.totalElementos").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }

    @Test
    void conviteRepetidoNaoGeraNotificacaoRepetida() throws Exception {
        Usuario cliente = novoCliente();
        Usuario prestador = novoPrestador();
        long projetoId = criarProjetoPublicado(cliente);

        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/projetos/" + projetoId + "/convites/" + prestador.getNomeUsuario()).with(csrf()).with(como(cliente)))
                    .andExpect(status().isNoContent());
        }

        mvc.perform(get("/api/notificacoes/nao-lidas").with(como(prestador)))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void cobrancaPixEhIdempotenteESoOClientePaga() throws Exception {
        Usuario cliente = novoCliente();
        Usuario prestador = novoPrestador();
        long contratacaoId = criarContratacao(cliente, prestador);

        long primeira = corpo(mvc.perform(post("/api/contratacoes/" + contratacaoId + "/pagamentos").with(csrf()).with(como(cliente)))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        mvc.perform(post("/api/contratacoes/" + contratacaoId + "/pagamentos").with(csrf()).with(como(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(primeira));

        mvc.perform(post("/api/contratacoes/" + contratacaoId + "/pagamentos").with(csrf()).with(como(prestador)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/debug/pagamentos/" + primeira + "/simular-pagamento").with(csrf()).with(como(prestador)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/debug/pagamentos/" + primeira + "/simular-pagamento").with(csrf()).with(como(novoCliente())))
                .andExpect(status().isNotFound());
    }

    @Test
    void statusDoPagamentoNaoPodeSerDefinidoPeloCliente() throws Exception {
        Usuario cliente = novoCliente();
        long contratacaoId = criarContratacao(cliente, novoPrestador());
        long pagamentoId = corpo(mvc.perform(post("/api/contratacoes/" + contratacaoId + "/pagamentos").with(csrf()).with(como(cliente)))
                .andReturn()).get("id").asLong();

        mvc.perform(put("/api/pagamentos/" + pagamentoId).with(csrf()).with(como(cliente)).contentType("application/json")
                        .content(json(Map.of("status", "PAID"))))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/pagamentos/" + pagamentoId).with(como(cliente)))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void cobrancaExpiradaNaoPodeSerPaga() throws Exception {
        Usuario cliente = novoCliente();
        long contratacaoId = criarContratacao(cliente, novoPrestador());
        long pagamentoId = corpo(mvc.perform(post("/api/contratacoes/" + contratacaoId + "/pagamentos").with(csrf()).with(como(cliente)))
                .andReturn()).get("id").asLong();

        mvc.perform(post("/api/debug/pagamentos/" + pagamentoId + "/simular-expiracao").with(csrf()).with(como(cliente)))
                .andExpect(jsonPath("$.status").value("EXPIRED"));
        mvc.perform(post("/api/debug/pagamentos/" + pagamentoId + "/simular-pagamento").with(csrf()).with(como(cliente)))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/contratacoes/" + contratacaoId + "/pagamentos").with(csrf()).with(como(cliente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not((int) pagamentoId)));
    }
}
