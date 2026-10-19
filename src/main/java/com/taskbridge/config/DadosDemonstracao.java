package com.taskbridge.config;

import com.taskbridge.model.enums.Disponibilidade;
import com.taskbridge.model.enums.ModalidadeTrabalho;
import com.taskbridge.model.enums.TipoConta;
import com.taskbridge.model.enums.TipoRequisito;
import com.taskbridge.model.Categoria;
import com.taskbridge.model.DadosProfissionais;
import com.taskbridge.model.DadosServico;
import com.taskbridge.model.EscopoProjeto;
import com.taskbridge.model.FaixaValor;
import com.taskbridge.model.PerfilProfissional;
import com.taskbridge.model.Projeto;
import com.taskbridge.model.Servico;
import com.taskbridge.model.Usuario;
import com.taskbridge.repository.CategoriaRepository;
import com.taskbridge.repository.PerfilProfissionalRepository;
import com.taskbridge.repository.ProjetoRepository;
import com.taskbridge.repository.ServicoRepository;
import com.taskbridge.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Popula o banco local com contas de exemplo para demonstração. Só existe fora de prod e com
 * {@code taskbridge.dados-demonstracao=true}. Os e-mails usam o domínio reservado {@code .test}.
 * A senha das contas de demonstração está documentada no README — nunca use em outro ambiente.
 */
@Component
@Profile("!prod")
@ConditionalOnProperty(name = "taskbridge.dados-demonstracao", havingValue = "true")
public class DadosDemonstracao implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosDemonstracao.class);
    static final String SENHA_DEMONSTRACAO = "demo12345678";

    private final UsuarioRepository usuarios;
    private final PerfilProfissionalRepository perfis;
    private final ServicoRepository servicos;
    private final ProjetoRepository projetos;
    private final CategoriaRepository categorias;
    private final PasswordEncoder passwordEncoder;
    private final PrivacidadeProperties privacidade;
    private final Clock clock;

    public DadosDemonstracao(UsuarioRepository usuarios, PerfilProfissionalRepository perfis, ServicoRepository servicos,
                             ProjetoRepository projetos, CategoriaRepository categorias, PasswordEncoder passwordEncoder,
                             PrivacidadeProperties privacidade, Clock clock) {
        this.usuarios = usuarios;
        this.perfis = perfis;
        this.servicos = servicos;
        this.projetos = projetos;
        this.categorias = categorias;
        this.passwordEncoder = passwordEncoder;
        this.privacidade = privacidade;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.existsByNomeUsuario("ana.cliente")) {
            return;
        }
        Map<String, Categoria> porSlug = categorias.findAll().stream()
                .collect(Collectors.toMap(Categoria::getSlug, Function.identity()));

        Usuario ana = conta("Ana Ribeiro", "ana.cliente", "ana@taskbridge.test", TipoConta.CLIENTE);
        ana.atualizarDadosPessoais("Ana Ribeiro", "Dona de uma pequena confeitaria em Campinas.", "Campinas, SP");

        Usuario joao = conta("João Silva", "joao.dev", "joao@taskbridge.test", TipoConta.PRESTADOR);
        joao.atualizarDadosPessoais("João Silva", "Desenvolvedor web há 8 anos, foco em sites institucionais e lojas.", "São Paulo, SP");
        perfis.save(new PerfilProfissional(joao, new DadosProfissionais("Desenvolvedor Web", "Sites rápidos e acessíveis.",
                8, Disponibilidade.IMEDIATA, ModalidadeTrabalho.REMOTO,
                new FaixaValor(new BigDecimal("1500"), new BigDecimal("6000")), List.of("html", "css", "javascript", "java", "seo"))));
        servicos.save(new Servico(joao, new DadosServico(porSlug.get("desenvolvimento-web"), "Site institucional completo",
                "Site responsivo de até 6 páginas, com formulário de contato e otimização básica para buscadores.",
                new BigDecimal("2500"), 20)));

        Usuario marina = conta("Marina Costa", "marina.design", "marina@taskbridge.test", TipoConta.PRESTADOR);
        perfis.save(new PerfilProfissional(marina, new DadosProfissionais("Designer de Identidade Visual", "Marcas para pequenos negócios.",
                5, Disponibilidade.EM_ATE_15_DIAS, ModalidadeTrabalho.HIBRIDO,
                new FaixaValor(new BigDecimal("800"), new BigDecimal("3500")), List.of("figma", "identidade visual", "ui"))));
        servicos.save(new Servico(marina, new DadosServico(porSlug.get("design-grafico"), "Identidade visual para pequenos negócios",
                "Logotipo, paleta, tipografia e manual de marca simplificado em PDF.",
                new BigDecimal("1800"), 15)));

        Projeto site = new Projeto(ana, EscopoProjeto.builder("Site para confeitaria artesanal",
                        "Preciso de um site para minha pequena empresa, para mostrar os produtos e receber encomendas.")
                .categoria(porSlug.get("desenvolvimento-web"))
                .objetivo("Receber encomendas pela internet e passar mais credibilidade.")
                .descricao("Site com página inicial, cardápio com fotos, página sobre a confeitaria e formulário de encomenda.")
                .orcamento(new FaixaValor(new BigDecimal("2000"), new BigDecimal("4000")))
                .prazoDias(30)
                .habilidades(List.of("html", "css", "seo"))
                .requisito(TipoRequisito.REQUISITO, "Funcionar bem no celular")
                .requisito(TipoRequisito.ENTREGAVEL, "Layout aprovado das páginas")
                .requisito(TipoRequisito.ENTREGAVEL, "Site publicado no domínio da empresa")
                .requisito(TipoRequisito.CRITERIO_ACEITE, "Formulário de encomenda enviando e-mail corretamente")
                .build());
        site.publicar(clock.instant());
        projetos.save(site);

        log.info("Dados de demonstração criados: ana.cliente, joao.dev, marina.design (ver README)");
    }

    private Usuario conta(String nome, String nomeUsuario, String email, TipoConta tipo) {
        return usuarios.save(new Usuario(nome, nomeUsuario, email, passwordEncoder.encode(SENHA_DEMONSTRACAO), tipo,
                new Usuario.AceiteTermos(privacidade.versaoTermos(), clock.instant())));
    }
}
