package com.taskbridge.security;

import com.taskbridge.model.Usuario;
import com.taskbridge.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario = usuarioRepository.findByEmail(Usuario.normalizarEmail(email))
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new UsernameNotFoundException("usuário não encontrado"));
        return new UsuarioAutenticado(usuario, usuario.estaBloqueado(clock.instant()));
    }
}
