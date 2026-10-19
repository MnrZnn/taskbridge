package com.taskbridge.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Com credenciais (fictícias) configuradas, os botões aparecem e o redirecionamento OAuth é montado. */
@TestPropertySource(properties = {
        "taskbridge.login-social.google.client-id=id-google-teste",
        "taskbridge.login-social.google.client-secret=segredo-teste",
        "taskbridge.login-social.github.client-id=id-github-teste",
        "taskbridge.login-social.github.client-secret=segredo-teste",
        "taskbridge.login-social.url-base=https://taskbridge.exemplo.test"
})
class LoginSocialConfiguradoIT extends TesteIntegracao {

    @Test
    void provedoresConfiguradosAparecemNaConfiguracaoPublica() throws Exception {
        mvc.perform(get("/api/config/publica"))
                .andExpect(jsonPath("$.provedoresLogin", containsInAnyOrder("GOOGLE", "GITHUB")));
    }

    @Test
    void googleRedirecionaComStateERedirectFixo() throws Exception {
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("https://accounts.google.com/o/oauth2/v2/auth")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("state=")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(
                        "redirect_uri=https://taskbridge.exemplo.test/login/oauth2/code/google")));
    }

    @Test
    void githubPedeEscopoDeEmail() throws Exception {
        mvc.perform(get("/oauth2/authorization/github"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("user:email")));
    }

    /** O login social não pode trocar o 401 em JSON da API por um redirecionamento para a página de login. */
    @Test
    void apiContinuaRespondendo401EmJson() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autenticação necessária."));
    }
}
