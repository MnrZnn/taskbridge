package com.taskbridge.repository;

import com.taskbridge.model.enums.ContratacaoStatus;
import com.taskbridge.model.enums.EntregaStatus;
import com.taskbridge.model.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    long countByContratacaoProjetoClienteIdAndStatusIn(Long clienteId, Collection<EntregaStatus> status);

    long countByContratacaoPropostaPrestadorIdAndContratacaoStatusAndStatusIn(
            Long prestadorId, ContratacaoStatus contratacaoStatus, Collection<EntregaStatus> status);
}
