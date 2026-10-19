package com.taskbridge.model;

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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Objetos de domínio prontos para testes unitários (sem banco). Ids são atribuídos como o JPA faria. */
public final class Fixtures {

    public static final Instant AGORA = Instant.parse("2026-09-01T12:00:00Z");
    private static final AtomicLong SEQUENCIA = new AtomicLong(1);

    private Fixtures() {
    }

    public static Usuario cliente() {
        return usuario(TipoConta.CLIENTE);
    }

    public static Usuario prestador() {
        return usuario(TipoConta.PRESTADOR);
    }

    private static Usuario usuario(TipoConta tipo) {
        long id = SEQUENCIA.getAndIncrement();
        Usuario usuario = new Usuario("Pessoa " + id, "pessoa" + id, "p" + id + "@x.test", "hash", tipo,
                new Usuario.AceiteTermos("v1", AGORA));
        return comId(usuario, id);
    }

    public static Categoria categoria(String slug) {
        Categoria categoria = instanciar(Categoria.class);
        ReflectionTestUtils.setField(categoria, "slug", slug);
        ReflectionTestUtils.setField(categoria, "nome", "Categoria " + slug);
        ReflectionTestUtils.setField(categoria, "sigla", "CAT");
        return comId(categoria, SEQUENCIA.getAndIncrement());
    }

    /** Escopo completo: passa em todas as pendências de publicação. */
    public static EscopoProjeto.Builder escopoCompleto(Categoria categoria) {
        return EscopoProjeto.builder("Site institucional", "Preciso de um site para minha empresa")
                .categoria(categoria)
                .objetivo("Ter presença online")
                .descricao("Site com quatro páginas, formulário de contato e versão para celular.")
                .orcamento(new FaixaValor(new BigDecimal("2000"), new BigDecimal("4000")))
                .prazoDias(30)
                .habilidades(List.of("html", "css"))
                .requisito(TipoRequisito.ENTREGAVEL, "Layout aprovado")
                .requisito(TipoRequisito.ENTREGAVEL, "Site publicado")
                .requisito(TipoRequisito.CRITERIO_ACEITE, "Formulário enviando e-mail");
    }

    public static Projeto projetoPublicado(Usuario cliente, Categoria categoria) {
        Projeto projeto = comId(new Projeto(cliente, escopoCompleto(categoria).build()), SEQUENCIA.getAndIncrement());
        projeto.publicar(AGORA);
        return projeto;
    }

    public static PerfilProfissional perfil(Usuario prestador, Disponibilidade disponibilidade, String minimo, String maximo, String... habilidades) {
        return new PerfilProfissional(prestador, new DadosProfissionais("Dev", null, 5, disponibilidade, ModalidadeTrabalho.REMOTO,
                new FaixaValor(new BigDecimal(minimo), new BigDecimal(maximo)), List.of(habilidades)));
    }

    public static Servico servico(Usuario prestador, Categoria categoria, int prazoDias) {
        return new Servico(prestador, new DadosServico(categoria, "Serviço de teste", "Descrição longa o bastante do serviço.",
                new BigDecimal("1000"), prazoDias));
    }

    public static <T> T comId(T entidade, long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }

    private static <T> T instanciar(Class<T> tipo) {
        try {
            var construtor = tipo.getDeclaredConstructor();
            construtor.setAccessible(true);
            return construtor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
