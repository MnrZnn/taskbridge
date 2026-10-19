package com.taskbridge.integracao;

import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.model.enums.ProvedorLogin;
import com.taskbridge.model.Usuario;
import com.taskbridge.repository.IdentidadeExternaRepository;
import com.taskbridge.repository.RegistroAuditoriaRepository;
import com.taskbridge.security.LoginSocialHandler;
import com.taskbridge.service.LoginSocialService;
import com.taskbridge.service.LoginSocialService.CadastroPendente;
import com.taskbridge.service.LoginSocialService.DadosProvedor;
import com.taskbridge.service.LoginSocialService.LoginSocialRecusadoException;
import com.taskbridge.service.LoginSocialService.Resultado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpSession;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginSocialIT extends TesteIntegracao {

    @Autowired
    private LoginSocialService loginSocial;

    @Autowired
    private IdentidadeExternaRepository identidades;

    @Autowired
    private RegistroAuditoriaRepository auditoria;

    private static DadosProvedor google(String sujeito, String email, boolean verificado) {
        return new DadosProvedor(ProvedorLogin.GOOGLE, sujeito, email, verificado, "Pessoa Google", email.split("@")[0]);
    }

    @Test
    void emailVerificadoIgualAoDeUmaContaVinculaEEntra() {
        Usuario existente = novoCliente();

        Resultado resultado = loginSocial.resolver(google("g-" + UUID.randomUUID(), existente.getEmail(), true));

        assertThat(resultado).isInstanceOf(Resultado.Entrar.class);
        assertThat(((Resultado.Entrar) resultado).usuario().getId()).isEqualTo(existente.getId());
        assertThat(identidades.existsByUsuarioIdAndProvedor(existente.getId(), ProvedorLogin.GOOGLE)).isTrue();
        assertThat(auditoria.findByUsuarioId(existente.getId(), Pageable.unpaged()))
                .extracting("evento").contains(EventoAuditoria.LOGIN_SOCIAL_VINCULADO);
    }

    @Test
    void identidadeJaVinculadaEntraMesmoSeOEmailMudouNoProvedor() {
        Usuario existente = novoCliente();
        String sujeito = "g-" + UUID.randomUUID();
        loginSocial.resolver(google(sujeito, existente.getEmail(), true));

        Resultado resultado = loginSocial.resolver(google(sujeito, "outro-" + UUID.randomUUID() + "@gmail.test", false));

        assertThat(((Resultado.Entrar) resultado).usuario().getId()).isEqualTo(existente.getId());
    }

    /** Sem essa trava, bastaria criar no provedor uma conta com o e-mail da vítima (sem verificar) para entrar na conta dela. */
    @Test
    void emailNaoVerificadoNuncaVinculaAContaExistente() {
        Usuario vitima = novoCliente();

        assertThatThrownBy(() -> loginSocial.resolver(google("atacante-" + UUID.randomUUID(), vitima.getEmail(), false)))
                .isInstanceOf(LoginSocialRecusadoException.class);
        assertThat(identidades.findByUsuarioId(vitima.getId())).isEmpty();
    }

    @Test
    void emailNovoPedeParaCompletarCadastro() {
        String email = "nova-" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.test";

        Resultado resultado = loginSocial.resolver(google("g-" + UUID.randomUUID(), email, true));

        CadastroPendente pendente = ((Resultado.CompletarCadastro) resultado).pendente();
        assertThat(pendente.email()).isEqualTo(email);
        assertThat(pendente.usuarioSugerido()).matches("[a-z0-9_.]{3,30}");
        assertThat(usuarioRepository.findByEmail(email)).isEmpty();
    }

    @Test
    void concluirCadastroCriaContaSemSenhaEJaEntra() throws Exception {
        String email = "social-" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.test";
        String usuarioNovo = "soc" + UUID.randomUUID().toString().substring(0, 8);
        MockHttpSession sessao = sessaoComPendente(email, Instant.now());

        mvc.perform(get("/api/auth/cadastro-social").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.sujeito").doesNotExist());

        mvc.perform(postJson("/api/auth/cadastro-social", Map.of("nomeCompleto", "Pessoa Social", "nomeUsuario", usuarioNovo,
                        "tipoConta", "PRESTADOR", "aceiteTermos", true)).session(sessao))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.possuiSenha").value(false));

        Usuario criado = usuarioRepository.findByEmail(email).orElseThrow();
        assertThat(criado.isPossuiSenha()).isFalse();
        assertThat(sessao.getAttribute(LoginSocialHandler.ATRIBUTO_CADASTRO_PENDENTE)).isNull();
        mvc.perform(get("/api/me").session(sessao)).andExpect(jsonPath("$.nomeUsuario").value(usuarioNovo));
        // Ninguém sabe a senha da conta social: login por senha não funciona até ela definir uma.
        mvc.perform(postJson("/api/auth/login", Map.of("email", email, "senha", "qualquer12345")).with(ipNovo()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void concluirCadastroExigeAceiteEDadosDaSessao() throws Exception {
        mvc.perform(postJson("/api/auth/cadastro-social", Map.of("nomeCompleto", "Sem sessão", "nomeUsuario", "semsessao",
                        "tipoConta", "CLIENTE", "aceiteTermos", true)))
                .andExpect(status().isNotFound());

        MockHttpSession sessao = sessaoComPendente("termos-" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.test", Instant.now());
        mvc.perform(postJson("/api/auth/cadastro-social", Map.of("nomeCompleto", "Sem aceite", "nomeUsuario", "semaceite1",
                        "tipoConta", "CLIENTE", "aceiteTermos", false)).session(sessao))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cadastroPendenteExpira() throws Exception {
        MockHttpSession sessao = sessaoComPendente("velho-" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.test",
                Instant.now().minus(20, ChronoUnit.MINUTES));

        mvc.perform(postJson("/api/auth/cadastro-social", Map.of("nomeCompleto", "Atrasado", "nomeUsuario", "atrasado1",
                        "tipoConta", "CLIENTE", "aceiteTermos", true)).session(sessao))
                .andExpect(status().is(422));
    }

    @Test
    void contaSemSenhaDefineAPrimeiraSemSenhaAtualMasNaoTrocaEmail() throws Exception {
        String email = "semsenha-" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.test";
        MockHttpSession sessao = sessaoComPendente(email, Instant.now());
        mvc.perform(postJson("/api/auth/cadastro-social", Map.of("nomeCompleto", "Sem Senha", "nomeUsuario",
                "ss" + UUID.randomUUID().toString().substring(0, 8), "tipoConta", "CLIENTE", "aceiteTermos", true)).session(sessao));

        mvc.perform(put("/api/me/email").with(csrf()).session(sessao).contentType("application/json")
                        .content(json(Map.of("novoEmail", "trocado@teste.test", "senhaAtual", "qualquer"))))
                .andExpect(status().is(422));
        mvc.perform(put("/api/me/senha").with(csrf()).session(sessao).contentType("application/json")
                        .content(json(Map.of("novaSenha", "primeiraSenha123"))))
                .andExpect(status().isNoContent());
        mvc.perform(postJson("/api/auth/login", Map.of("email", email, "senha", "primeiraSenha123")).with(ipNovo()))
                .andExpect(status().isOk());
    }

    @Test
    void contaComSenhaContinuaExigindoASenhaAtual() throws Exception {
        mvc.perform(put("/api/me/senha").with(csrf()).with(como(novoCliente())).contentType("application/json")
                        .content(json(Map.of("novaSenha", "outraSenha123"))))
                .andExpect(status().is(422));
    }

    @Test
    void semCredenciaisConfiguradasNenhumProvedorAparece() throws Exception {
        mvc.perform(get("/api/config/publica")).andExpect(jsonPath("$.provedoresLogin").isEmpty());
    }

    private MockHttpSession sessaoComPendente(String email, Instant criadoEm) {
        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute(LoginSocialHandler.ATRIBUTO_CADASTRO_PENDENTE, new CadastroPendente(ProvedorLogin.GITHUB,
                "gh-" + UUID.randomUUID(), email, "Pessoa Social", "pessoa", criadoEm));
        return sessao;
    }
}
