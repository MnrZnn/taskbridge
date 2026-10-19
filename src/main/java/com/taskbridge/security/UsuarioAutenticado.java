package com.taskbridge.security;

import com.taskbridge.model.enums.TipoConta;
import com.taskbridge.model.Usuario;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/**
 * Principal guardado na sessão. Contém só o necessário para autorizar requisições;
 * o hash da senha é apagado logo após a autenticação ({@link #eraseCredentials()}).
 */
public class UsuarioAutenticado implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private final TipoConta tipoConta;
    private final boolean bloqueado;
    private String senhaHash;

    public UsuarioAutenticado(Usuario usuario, boolean bloqueado) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.tipoConta = usuario.getTipoConta();
        this.senhaHash = usuario.getSenhaHash();
        this.bloqueado = bloqueado;
    }

    public Long getId() {
        return id;
    }

    public TipoConta getTipoConta() {
        return tipoConta;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(tipoConta.authority()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !bloqueado;
    }

    @Override
    public void eraseCredentials() {
        this.senhaHash = null;
    }

    @Override
    public String toString() {
        return "UsuarioAutenticado[id=" + id + ", tipo=" + tipoConta + "]";
    }
}
