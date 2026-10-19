package com.taskbridge.model;

import com.taskbridge.model.enums.TipoNotificacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notificacao")
public class Notificacao {

    private static final int TAMANHO_MAXIMO_MENSAGEM = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoNotificacao tipo;

    @Column(nullable = false, length = TAMANHO_MAXIMO_MENSAGEM)
    private String mensagem;

    /** Caminho interno (ex.: /contracts/12). Nunca uma URL externa, para não virar vetor de open redirect. */
    @Column(length = 200)
    private String link;

    @Column(nullable = false)
    private boolean lida;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    protected Notificacao() {
    }

    public Notificacao(Usuario destinatario, TipoNotificacao tipo, String tituloProjeto, String link, Instant agora) {
        this.destinatario = destinatario;
        this.tipo = tipo;
        this.mensagem = limitar(tipo.mensagemPara(tituloProjeto));
        this.link = link;
        this.criadaEm = agora;
    }

    private static String limitar(String texto) {
        return texto.length() <= TAMANHO_MAXIMO_MENSAGEM ? texto : texto.substring(0, TAMANHO_MAXIMO_MENSAGEM - 1) + "…";
    }

    public void marcarComoLida() {
        this.lida = true;
    }

    public boolean pertenceA(Long usuarioId) {
        return destinatario.getId().equals(usuarioId);
    }

    public Long getId() {
        return id;
    }

    public TipoNotificacao getTipo() {
        return tipo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public String getLink() {
        return link;
    }

    public boolean isLida() {
        return lida;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}
