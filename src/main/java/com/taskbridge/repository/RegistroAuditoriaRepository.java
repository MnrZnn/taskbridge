package com.taskbridge.repository;

import com.taskbridge.model.RegistroAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    Page<RegistroAuditoria> findByUsuarioId(Long usuarioId, Pageable pageable);

    @Modifying
    @Query("delete from RegistroAuditoria r where r.criadoEm < :limite")
    int excluirAnterioresA(@Param("limite") Instant limite);
}
