package com.taskbridge.repository;

import com.taskbridge.model.enums.ServicoStatus;
import com.taskbridge.model.Servico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    @Query(value = """
            select s from Servico s join fetch s.prestador p join fetch s.categoria
            where s.status = com.taskbridge.model.enums.ServicoStatus.ATIVO
              and p.status = com.taskbridge.model.enums.UsuarioStatus.ATIVO
              and (:categoriaId is null or s.categoria.id = :categoriaId)
            """,
            countQuery = """
            select count(s) from Servico s
            where s.status = com.taskbridge.model.enums.ServicoStatus.ATIVO
              and s.prestador.status = com.taskbridge.model.enums.UsuarioStatus.ATIVO
              and (:categoriaId is null or s.categoria.id = :categoriaId)
            """)
    Page<Servico> buscarAtivos(@Param("categoriaId") Long categoriaId, Pageable pageable);

    @EntityGraph(attributePaths = {"categoria", "prestador"})
    Page<Servico> findByPrestadorId(Long prestadorId, Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    List<Servico> findByPrestadorIdAndStatusOrderByCriadoEmDesc(Long prestadorId, ServicoStatus status);

    @EntityGraph(attributePaths = "categoria")
    List<Servico> findByPrestadorIdInAndStatus(Collection<Long> prestadorIds, ServicoStatus status);

    long countByPrestadorIdAndStatus(Long prestadorId, ServicoStatus status);

    List<Servico> findByPrestadorId(Long prestadorId);
}
