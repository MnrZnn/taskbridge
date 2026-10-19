package com.taskbridge.dao;

import com.taskbridge.model.enums.ProjetoStatus;
import com.taskbridge.model.Projeto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * DAO da busca pública de projetos. Existe separado do {@code ProjetoRepository} porque os filtros
 * são opcionais e combináveis — com métodos derivados seriam uma explosão de variações.
 * <p>
 * Tudo passa pela Criteria API: os valores viram parâmetros vinculados, nunca texto SQL.
 * O termo de busca ainda tem os curingas do LIKE escapados, senão "%" digitado pelo usuário
 * viraria uma busca por "qualquer coisa" (não é injeção, mas é uma consulta que ele não pediu).
 */
@Repository
public class ProjetoBuscaDao {

    private static final Set<ProjetoStatus> STATUS_ABERTOS = Set.of(ProjetoStatus.PUBLISHED, ProjetoStatus.IN_NEGOTIATION);
    private static final char ESCAPE = '\\';

    @PersistenceContext
    private EntityManager entityManager;

    public Page<Projeto> buscarAbertos(FiltroProjetos filtro, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Projeto> consulta = cb.createQuery(Projeto.class);
        Root<Projeto> projeto = consulta.from(Projeto.class);
        projeto.fetch("categoria");
        consulta.select(projeto)
                .where(filtros(cb, projeto, filtro))
                .orderBy(cb.desc(projeto.get("publicadoEm")), cb.desc(projeto.get("id")));

        List<Projeto> pagina = entityManager.createQuery(consulta)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> contagem = cb.createQuery(Long.class);
        Root<Projeto> projetoContado = contagem.from(Projeto.class);
        contagem.select(cb.count(projetoContado)).where(filtros(cb, projetoContado, filtro));
        long total = entityManager.createQuery(contagem).getSingleResult();

        return new PageImpl<>(pagina, pageable, total);
    }

    private Predicate[] filtros(CriteriaBuilder cb, Root<Projeto> projeto, FiltroProjetos filtro) {
        List<Predicate> predicados = new ArrayList<>();
        predicados.add(projeto.get("status").in(STATUS_ABERTOS));
        if (filtro.categoriaId() != null) {
            predicados.add(cb.equal(projeto.get("categoria").get("id"), filtro.categoriaId()));
        }
        if (filtro.temTexto()) {
            String padrao = "%" + escaparLike(filtro.texto().toLowerCase(Locale.ROOT)) + "%";
            predicados.add(cb.or(
                    cb.like(cb.lower(projeto.get("titulo")), padrao, ESCAPE),
                    cb.like(cb.lower(projeto.get("descricao")), padrao, ESCAPE)));
        }
        if (filtro.orcamentoMinimo() != null) {
            predicados.add(cb.greaterThanOrEqualTo(projeto.get("orcamento").get("maximo"), filtro.orcamentoMinimo()));
        }
        return predicados.toArray(Predicate[]::new);
    }

    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public record FiltroProjetos(Long categoriaId, String texto, BigDecimal orcamentoMinimo) {

        public boolean temTexto() {
            return texto != null && !texto.isBlank();
        }
    }
}
