package com.taskbridge.repository;

import com.taskbridge.model.enums.TipoNotificacao;
import com.taskbridge.model.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    Page<Notificacao> findByDestinatarioId(Long usuarioId, Pageable pageable);

    long countByDestinatarioIdAndLidaFalse(Long usuarioId);

    boolean existsByDestinatarioIdAndTipoAndLink(Long usuarioId, TipoNotificacao tipo, String link);

    @Modifying
    @Query("update Notificacao n set n.lida = true where n.destinatario.id = :usuarioId and n.lida = false")
    int marcarTodasComoLidas(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("delete from Notificacao n where n.destinatario.id = :usuarioId")
    int excluirDoUsuario(@Param("usuarioId") Long usuarioId);
}
