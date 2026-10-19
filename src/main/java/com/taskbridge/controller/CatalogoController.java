package com.taskbridge.controller;

import com.taskbridge.config.PagamentoProperties;
import com.taskbridge.config.PrivacidadeProperties;
import com.taskbridge.dto.response.CategoriaResponse;
import com.taskbridge.dto.response.ConfiguracaoPublicaResponse;
import com.taskbridge.security.ProvedoresLoginSocial;
import com.taskbridge.service.CategoriaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Catálogo", description = "Dados de referência públicos")
public class CatalogoController {

    private final CategoriaService categoriaService;
    private final PagamentoProperties pagamento;
    private final PrivacidadeProperties privacidade;
    private final Environment ambiente;
    private final ProvedoresLoginSocial provedoresSociais;

    public CatalogoController(CategoriaService categoriaService, PagamentoProperties pagamento,
                              PrivacidadeProperties privacidade, Environment ambiente,
                              ProvedoresLoginSocial provedoresSociais) {
        this.categoriaService = categoriaService;
        this.pagamento = pagamento;
        this.privacidade = privacidade;
        this.ambiente = ambiente;
        this.provedoresSociais = provedoresSociais;
    }

    @GetMapping("/api/categorias")
    public List<CategoriaResponse> categorias() {
        return categoriaService.listar().stream().map(CategoriaResponse::de).toList();
    }

    /** Diz ao front-end se deve mostrar os botões de simulação do PIX. Não expõe nenhum segredo. */
    @GetMapping("/api/config/publica")
    public ConfiguracaoPublicaResponse configuracao() {
        boolean simulacao = pagamento.debugHabilitado() && !ambiente.acceptsProfiles(Profiles.of("prod"));
        return new ConfiguracaoPublicaResponse(pagamento.modo(), simulacao, privacidade.versaoTermos(),
                provedoresSociais.ativos());
    }
}
