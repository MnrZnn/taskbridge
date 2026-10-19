package com.taskbridge.repository;

import com.taskbridge.model.enums.ProjetoStatus;
import com.taskbridge.model.Projeto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {

    @EntityGraph(attributePaths = "categoria")
    Page<Projeto> findByClienteId(Long clienteId, Pageable pageable);

    /**
     * SELECT ... FOR UPDATE: aceitar proposta trava a linha do projeto até o fim da transação.
     * Dois aceites simultâneos no mesmo projeto ficam em fila; o segundo encontra o projeto
     * já CONTRACTED e falha na transição de estado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Projeto p where p.id = :id")
    Optional<Projeto> buscarParaAtualizacao(@Param("id") Long id);

    @EntityGraph(attributePaths = {"categoria", "cliente"})
    List<Projeto> findByStatusInOrderByPublicadoEmDesc(Collection<ProjetoStatus> status, Pageable limite);

    List<Projeto> findByClienteIdAndStatusIn(Long clienteId, Collection<ProjetoStatus> status);

    @EntityGraph(attributePaths = "categoria")
    List<Projeto> findByClienteIdAndStatusInOrderByCriadoEmDesc(Long clienteId, Collection<ProjetoStatus> status, Pageable limite);

    long countByClienteIdAndStatusIn(Long clienteId, Collection<ProjetoStatus> status);

    long countByClienteIdAndStatus(Long clienteId, ProjetoStatus status);
}
