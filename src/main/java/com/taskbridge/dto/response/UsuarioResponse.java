package com.taskbridge.dto.response;

import com.taskbridge.model.enums.TipoConta;
import com.taskbridge.model.Usuario;

import java.time.Instant;
import java.util.List;

/**
 * Dados da própria conta. Nunca inclui hash de senha, tentativas de login ou estado de bloqueio.
 * {@code possuiSenha} só diz se a conta tem senha própria (contas de login social podem não ter).
 */
public record UsuarioResponse(
        Long id,
        String nomeCompleto,
        String nomeUsuario,
        String email,
        TipoConta tipoConta,
        String biografia,
        String localizacao,
        String fotoUrl,
        List<String> links,
        boolean possuiSenha,
        Instant dataCadastro) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNomeCompleto(),
                usuario.getNomeUsuario(),
                usuario.getEmail(),
                usuario.getTipoConta(),
                usuario.getBiografia(),
                usuario.getLocalizacao(),
                UsuarioResumoResponse.fotoUrl(usuario),
                usuario.getLinks(),
                usuario.isPossuiSenha(),
                usuario.getDataCadastro());
    }
}
