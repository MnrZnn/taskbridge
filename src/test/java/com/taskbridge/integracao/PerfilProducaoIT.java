package com.taskbridge.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Com o perfil prod ativo, as rotas de simulação de pagamento e a documentação da API não existem,
 * mesmo que alguém tente ligar a simulação por variável de ambiente.
 */
@ActiveProfiles(value = {"test", "prod"})
class PerfilProducaoIT extends TesteIntegracao {

    @Test
    void endpointsDeDebugNaoExistemEmProducao() throws Exception {
        mvc.perform(post("/api/debug/pagamentos/1/simular-pagamento").with(csrf()).with(como(novoCliente())))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/config/publica"))
                .andExpect(jsonPath("$.simulacaoPagamentoHabilitada").value(false));
    }

    @Test
    void documentacaoDaApiDesligadaEmProducao() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }
}
