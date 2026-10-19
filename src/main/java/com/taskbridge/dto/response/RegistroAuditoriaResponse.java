package com.taskbridge.dto.response;

import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.model.enums.TipoRecurso;
import com.taskbridge.model.RegistroAuditoria;

import java.time.Instant;

public record RegistroAuditoriaResponse(EventoAuditoria evento, TipoRecurso recursoTipo, Long recursoId, String ip, Instant criadoEm) {

    public static RegistroAuditoriaResponse de(RegistroAuditoria registro) {
        return new RegistroAuditoriaResponse(registro.getEvento(), registro.getRecursoTipo(), registro.getRecursoId(),
                registro.getIp(), registro.getCriadoEm());
    }
}
