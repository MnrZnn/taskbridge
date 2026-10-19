package com.taskbridge.repository;

import com.taskbridge.model.enums.PagamentoStatus;
import com.taskbridge.model.Pagamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    /** Confirmação, expiração e cancelamento podem concorrer (usuário x agendador): trava a linha. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pagamento p where p.id = :id")
    Optional<Pagamento> buscarParaAtualizacao(@Param("id") Long id);

    List<Pagamento> findByContratacaoIdAndStatusIn(Long contratacaoId, Collection<PagamentoStatus> status);

    List<Pagamento> findByContratacaoIdOrderByCriadoEmDesc(Long contratacaoId);

    @Query("select p.id from Pagamento p where p.status in :status and p.expiraEm <= :agora order by p.expiraEm")
    List<Long> idsVencidos(@Param("status") Collection<PagamentoStatus> status,
                           @Param("agora") Instant agora,
                           Pageable lote);
}
