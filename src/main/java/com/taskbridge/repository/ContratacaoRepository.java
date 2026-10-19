package com.taskbridge.repository;

import com.taskbridge.model.enums.ContratacaoStatus;
import com.taskbridge.model.Contratacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ContratacaoRepository extends JpaRepository<Contratacao, Long> {

    @Query(value = """
            select c from Contratacao c join fetch c.projeto p join fetch c.proposta pr
            where p.cliente.id = :usuarioId or pr.prestador.id = :usuarioId
            """,
            countQuery = """
            select count(c) from Contratacao c
            where c.projeto.cliente.id = :usuarioId or c.proposta.prestador.id = :usuarioId
            """)
    Page<Contratacao> buscarPorParticipante(@Param("usuarioId") Long usuarioId, Pageable pageable);

    @Query("""
            select count(c) > 0 from Contratacao c
            where (c.projeto.cliente.id = :usuarioId or c.proposta.prestador.id = :usuarioId)
              and c.status in :status
            """)
    boolean existeComParticipanteEStatus(@Param("usuarioId") Long usuarioId,
                                         @Param("status") Collection<ContratacaoStatus> status);

    @Query("""
            select c from Contratacao c join fetch c.projeto p join fetch c.proposta pr
            where (p.cliente.id = :usuarioId or pr.prestador.id = :usuarioId) and c.status in :status
            order by c.criadaEm desc
            """)
    List<Contratacao> buscarPorParticipanteEStatus(@Param("usuarioId") Long usuarioId,
                                                   @Param("status") Collection<ContratacaoStatus> status,
                                                   Pageable limite);

    @EntityGraph(attributePaths = {"projeto", "projeto.categoria"})
    List<Contratacao> findByPropostaPrestadorIdAndStatusOrderByConcluidaEmDesc(Long prestadorId, ContratacaoStatus status, Pageable limite);

    @EntityGraph(attributePaths = {"projeto", "projeto.categoria"})
    List<Contratacao> findByProjetoClienteIdAndStatusOrderByConcluidaEmDesc(Long clienteId, ContratacaoStatus status, Pageable limite);

    long countByProjetoClienteIdAndStatus(Long clienteId, ContratacaoStatus status);

    long countByProjetoClienteIdAndStatusIn(Long clienteId, Collection<ContratacaoStatus> status);

    long countByPropostaPrestadorIdAndStatusIn(Long prestadorId, Collection<ContratacaoStatus> status);

    long countByPropostaPrestadorIdAndStatus(Long prestadorId, ContratacaoStatus status);
}
