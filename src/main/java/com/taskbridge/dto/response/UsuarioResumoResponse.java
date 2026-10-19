package com.taskbridge.dto.response;

import com.taskbridge.model.Usuario;

/** Visão pública mínima de um usuário, usada quando ele aparece em recursos de outras pessoas. Sem e-mail. */
public record UsuarioResumoResponse(String nomeUsuario, String nomeCompleto, String fotoUrl) {

    public static UsuarioResumoResponse de(Usuario usuario) {
        return new UsuarioResumoResponse(usuario.getNomeUsuario(), usuario.getNomeCompleto(), fotoUrl(usuario));
    }

    static String fotoUrl(Usuario usuario) {
        return usuario.getFotoPerfil() == null ? null : "/api/arquivos/fotos/" + usuario.getFotoPerfil();
    }
}
