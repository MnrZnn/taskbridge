package com.taskbridge.model;

import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.model.enums.TipoRecurso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Registro imutável de evento relevante. Guarda o quê, quem (id), sobre qual recurso e de onde —
 * nunca senha, token, e-mail ou conteúdo enviado pelo usuário.
 */
@Entity
@Table(name = "registro_auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private EventoAuditoria evento;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, updatable = false)
    private TipoRecurso recursoTipo;

    @Column(updatable = false)
    private Long recursoId;

    @Column(length = 45, updatable = false)
    private String ip;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    protected RegistroAuditoria() {
    }

    public RegistroAuditoria(EventoAuditoria evento, Long usuarioId, TipoRecurso recursoTipo, Long recursoId,
                             String ip, Instant agora) {
        this.evento = evento;
        this.usuarioId = usuarioId;
        this.recursoTipo = recursoTipo;
        this.recursoId = recursoId;
        this.ip = ip;
        this.criadoEm = agora;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public EventoAuditoria getEvento() {
        return evento;
    }

    public TipoRecurso getRecursoTipo() {
        return recursoTipo;
    }

    public Long getRecursoId() {
        return recursoId;
    }

    public String getIp() {
        return ip;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
