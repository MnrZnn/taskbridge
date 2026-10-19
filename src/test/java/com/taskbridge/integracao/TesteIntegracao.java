package com.taskbridge.integracao;

import com.taskbridge.model.enums.TipoConta;
import com.taskbridge.model.Usuario;
import com.taskbridge.repository.UsuarioRepository;
import com.taskbridge.security.UsuarioAutenticado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração: contexto completo, H2 com as migrations reais e MockMvc
 * passando pela cadeia do Spring Security. Cada teste cria seus próprios usuários (nomes únicos),
 * então os testes não dependem de ordem nem de limpeza do banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class TesteIntegracao {

    protected static final String SENHA = "senhaSegura123";
    private static final AtomicInteger IPS = new AtomicInteger(1);

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JsonMapper jsonMapper;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    protected Usuario novoCliente() {
        return novoUsuario(TipoConta.CLIENTE);
    }

    protected Usuario novoPrestador() {
        return novoUsuario(TipoConta.PRESTADOR);
    }

    private Usuario novoUsuario(TipoConta tipo) {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        return usuarioRepository.save(new Usuario("Usuário " + sufixo, "u" + sufixo, "u" + sufixo + "@teste.test",
                passwordEncoder.encode(SENHA), tipo, new Usuario.AceiteTermos("teste", java.time.Instant.now())));
    }

    /** Autentica a requisição como o usuário, sem passar pelo login (que tem rate limit por IP). */
    protected static RequestPostProcessor como(Usuario usuario) {
        return user(new UsuarioAutenticado(usuario, false));
    }

    /** IP diferente por teste para que o rate limit de login de um teste não afete outro. */
    protected static RequestPostProcessor ipNovo() {
        String ip = "10.1." + (IPS.get() / 250) + "." + (IPS.getAndIncrement() % 250 + 1);
        return requisicao -> {
            requisicao.setRemoteAddr(ip);
            return requisicao;
        };
    }

    protected MockHttpServletRequestBuilder postJson(String url, Object corpo) {
        return post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(corpo));
    }

    protected String json(Object objeto) {
        return jsonMapper.writeValueAsString(objeto);
    }

    protected JsonNode corpo(MvcResult resultado) throws Exception {
        return jsonMapper.readTree(resultado.getResponse().getContentAsString());
    }

    protected static Map<String, Object> projetoCompleto(String titulo) {
        return Map.of(
                "titulo", titulo,
                "necessidade", "Preciso de um site para a minha empresa",
                "categoriaId", 1,
                "objetivo", "Ter presença online",
                "descricao", "Site com quatro páginas, formulário de contato e versão para celular.",
                "orcamentoMinimo", 2000,
                "orcamentoMaximo", 4000,
                "prazoDias", 30,
                "habilidades", List.of("html", "css"),
                "requisitos", List.of(
                        Map.of("tipo", "ENTREGAVEL", "descricao", "Site publicado"),
                        Map.of("tipo", "CRITERIO_ACEITE", "descricao", "Formulário funcionando")));
    }

    protected long criarRascunho(Usuario cliente) throws Exception {
        MvcResult resultado = mvc.perform(postJson("/api/projetos", projetoCompleto("Projeto de teste")).with(como(cliente)))
                .andExpect(status().isCreated())
                .andReturn();
        return corpo(resultado).get("id").asLong();
    }

    protected long criarProjetoPublicado(Usuario cliente) throws Exception {
        long id = criarRascunho(cliente);
        mvc.perform(post("/api/projetos/" + id + "/publicar").with(csrf()).with(como(cliente)))
                .andExpect(status().isOk());
        return id;
    }

    protected long enviarProposta(Usuario prestador, long projetoId) throws Exception {
        MvcResult resultado = mvc.perform(postJson("/api/projetos/" + projetoId + "/propostas",
                        Map.of("valor", 3000, "prazoDias", 20, "mensagem", "Faço o site completo em duas etapas."))
                        .with(como(prestador)))
                .andExpect(status().isCreated())
                .andReturn();
        return corpo(resultado).get("id").asLong();
    }

    /** Projeto publicado → proposta → aceite. Devolve o id da contratação. */
    protected long criarContratacao(Usuario cliente, Usuario prestador) throws Exception {
        long projetoId = criarProjetoPublicado(cliente);
        long propostaId = enviarProposta(prestador, projetoId);
        MvcResult resultado = mvc.perform(post("/api/propostas/" + propostaId + "/aceitar").with(csrf()).with(como(cliente)))
                .andExpect(status().isCreated())
                .andReturn();
        return corpo(resultado).get("id").asLong();
    }
}
