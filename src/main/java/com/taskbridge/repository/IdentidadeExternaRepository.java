package com.taskbridge.repository;

import com.taskbridge.model.enums.ProvedorLogin;
import com.taskbridge.model.IdentidadeExterna;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IdentidadeExternaRepository extends JpaRepository<IdentidadeExterna, Long> {

    @EntityGraph(attributePaths = "usuario")
    Optional<IdentidadeExterna> findByProvedorAndSujeito(ProvedorLogin provedor, String sujeito);

    boolean existsByUsuarioIdAndProvedor(Long usuarioId, ProvedorLogin provedor);

    List<IdentidadeExterna> findByUsuarioId(Long usuarioId);

    @Modifying
    @Query("delete from IdentidadeExterna i where i.usuario.id = :usuarioId")
    int excluirDoUsuario(@Param("usuarioId") Long usuarioId);
}
