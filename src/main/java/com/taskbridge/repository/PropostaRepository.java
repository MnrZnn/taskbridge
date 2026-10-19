package com.taskbridge.repository;

import com.taskbridge.model.enums.PropostaStatus;
import com.taskbridge.model.Proposta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {

    @EntityGraph(attributePaths = {"prestador", "projeto"})
    Page<Proposta> findByProjetoId(Long projetoId, Pageable pageable);

    @EntityGraph(attributePaths = {"prestador", "projeto"})
    Page<Proposta> findByPrestadorId(Long prestadorId, Pageable pageable);

    @EntityGraph(attributePaths = {"prestador", "projeto"})
    Page<Proposta> findByProjetoClienteId(Long clienteId, Pageable pageable);

    List<Proposta> findByProjetoIdAndStatus(Long projetoId, PropostaStatus status);

    List<Proposta> findByPrestadorIdAndStatus(Long prestadorId, PropostaStatus status);

    boolean existsByProjetoIdAndPrestadorId(Long projetoId, Long prestadorId);

    long countByPrestadorIdAndStatus(Long prestadorId, PropostaStatus status);

    long countByProjetoClienteIdAndStatus(Long clienteId, PropostaStatus status);

    @Query("select p.projeto.id from Proposta p where p.prestador.id = :prestadorId")
    List<Long> idsDeProjetosComPropostaDe(@Param("prestadorId") Long prestadorId);
}
