package com.taskbridge.dto.response;

import com.taskbridge.model.enums.ProvedorLogin;
import com.taskbridge.service.LoginSocialService.CadastroPendente;

/** O que a tela "complete seu cadastro" mostra. Sem o id do usuário no provedor. */
public record CadastroSocialResponse(ProvedorLogin provedor, String provedorRotulo, String email, String nomeSugerido,
                                     String usuarioSugerido) {

    public static CadastroSocialResponse de(CadastroPendente pendente) {
        return new CadastroSocialResponse(pendente.provedor(), pendente.provedor().rotulo(), pendente.email(),
                pendente.nomeSugerido(), pendente.usuarioSugerido());
    }
}
