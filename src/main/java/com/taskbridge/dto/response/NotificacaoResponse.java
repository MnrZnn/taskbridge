package com.taskbridge.dto.response;

import com.taskbridge.model.enums.TipoNotificacao;
import com.taskbridge.model.Notificacao;

import java.time.Instant;

public record NotificacaoResponse(Long id, TipoNotificacao tipo, String mensagem, String link, boolean lida, Instant criadaEm) {

    public static NotificacaoResponse de(Notificacao notificacao) {
        return new NotificacaoResponse(notificacao.getId(), notificacao.getTipo(), notificacao.getMensagem(),
                notificacao.getLink(), notificacao.isLida(), notificacao.getCriadaEm());
    }
}
