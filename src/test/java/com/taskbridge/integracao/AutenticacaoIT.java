package com.taskbridge.integracao;

import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.model.Usuario;
import com.taskbridge.repository.RegistroAuditoriaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutenticacaoIT extends TesteIntegracao {

    @Autowired
    private RegistroAuditoriaRepository auditoria;

    private Map<String, Object> registro(String usuario) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nomeCompleto", "Pessoa de Teste");
        corpo.put("nomeUsuario", usuario);
        corpo.put("email", usuario.toUpperCase() + "@Teste.test");
        corpo.put("senha", SENHA);
        corpo.put("tipoConta", "CLIENTE");
        corpo.put("aceiteTermos", true);
        return corpo;
    }

    @Test
    void cadastroNaoDevolveSenhaENormalizaEmail() throws Exception {
        mvc.perform(postJson("/api/auth/registro", registro("cadastro1")).with(ipNovo()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("cadastro1@teste.test"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        Usuario salvo = usuarioRepository.findByEmail("cadastro1@teste.test").orElseThrow();
        assertThat(salvo.getSenhaHash()).startsWith("$2").isNotEqualTo(SENHA);
    }

    @Test
    void cadastroExigeAceiteDosTermos() throws Exception {
        Map<String, Object> corpo = registro("semaceite");
        corpo.put("aceiteTermos", false);

        mvc.perform(postJson("/api/auth/registro", corpo).with(ipNovo()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.aceiteTermos").exists());
    }

    @Test
    void cadastroDuplicadoNaoDizQualCampoExiste() throws Exception {
        mvc.perform(postJson("/api/auth/registro", registro("duplicado1")).with(ipNovo())).andExpect(status().isCreated());

        mvc.perform(postJson("/api/auth/registro", registro("duplicado1")).with(ipNovo()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("E-mail ou nome de usuário indisponível."));
    }

    @Test
    void senhaErradaEEmailInexistenteTemAMesmaResposta() throws Exception {
        Usuario usuario = novoCliente();

        MvcResult senhaErrada = mvc.perform(postJson("/api/auth/login", Map.of("email", usuario.getEmail(), "senha", "errada123456")).with(ipNovo()))
                .andExpect(status().isUnauthorized()).andReturn();
        MvcResult emailInexistente = mvc.perform(postJson("/api/auth/login", Map.of("email", "ninguem@teste.test", "senha", "errada123456")).with(ipNovo()))
                .andExpect(status().isUnauthorized()).andReturn();

        assertThat(corpo(senhaErrada).get("message").asString()).isEqualTo(corpo(emailInexistente).get("message").asString());
    }

    @Test
    void loginCorretoCriaSessaoERegistraAuditoria() throws Exception {
        Usuario usuario = novoCliente();

        mvc.perform(postJson("/api/auth/login", Map.of("email", usuario.getEmail(), "senha", SENHA)).with(ipNovo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeUsuario").value(usuario.getNomeUsuario()))
                .andExpect(result -> assertThat(result.getRequest().getSession(false)).isNotNull());

        assertThat(auditoria.findByUsuarioId(usuario.getId(), Pageable.unpaged()))
                .extracting("evento").contains(EventoAuditoria.LOGIN_SUCESSO);
    }

    /** Ataque distribuído: cada tentativa de um IP diferente, então o rate limit por IP não pega — o bloqueio da conta pega. */
    @Test
    void contaBloqueiaAposCincoFalhasMesmoComSenhaCorretaDepois() throws Exception {
        Usuario usuario = novoCliente();
        for (int i = 0; i < 5; i++) {
            mvc.perform(postJson("/api/auth/login", Map.of("email", usuario.getEmail(), "senha", "tentativa" + i + "abc")).with(ipNovo()))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(postJson("/api/auth/login", Map.of("email", usuario.getEmail(), "senha", SENHA)).with(ipNovo()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));

        assertThat(auditoria.findByUsuarioId(usuario.getId(), Pageable.unpaged()))
                .extracting("evento").contains(EventoAuditoria.LOGIN_CONTA_BLOQUEADA);
    }

    @Test
    void loginTemRateLimitPorIp() throws Exception {
        var mesmoIp = ipNovo();
        for (int i = 0; i < 5; i++) {
            mvc.perform(postJson("/api/auth/login", Map.of("email", "x" + i + "@teste.test", "senha", "qualquer123")).with(mesmoIp))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(postJson("/api/auth/login", Map.of("email", "y@teste.test", "senha", "qualquer123")).with(mesmoIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void requisicaoQueAlteraEstadoSemTokenCsrfEhRecusada() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "a@teste.test", "senha", "qualquer123"))).with(ipNovo()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("CSRF")));
    }

    @Test
    void apiPrivadaExigeAutenticacao() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/contratacoes")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEncerraSessao() throws Exception {
        mvc.perform(post("/api/auth/logout").with(csrf()).with(como(novoCliente())))
                .andExpect(status().isNoContent());
    }

    @Test
    void paginasTemCabecalhosDeSeguranca() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("script-src 'self'")))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void healthCheckNaoExpoeDetalhes() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }
}
