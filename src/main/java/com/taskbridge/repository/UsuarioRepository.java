package com.taskbridge.repository;

import com.taskbridge.model.enums.UsuarioStatus;
import com.taskbridge.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByNomeUsuarioAndStatus(String nomeUsuario, UsuarioStatus status);

    boolean existsByEmail(String email);

    boolean existsByNomeUsuario(String nomeUsuario);
}
