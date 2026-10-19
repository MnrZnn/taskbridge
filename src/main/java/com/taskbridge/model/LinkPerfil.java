package com.taskbridge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Link público do perfil (LinkedIn, GitHub, portfólio...). Criado só por {@link Usuario#redefinirLinks}. */
@Entity
@Table(name = "usuario_link")
public class LinkPerfil {

    public static final int MAXIMO_POR_USUARIO = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = UrlPublica.TAMANHO_MAXIMO)
    private String url;

    @Column(nullable = false)
    private int ordem;

    protected LinkPerfil() {
    }

    LinkPerfil(Usuario usuario, String url, int ordem) {
        this.usuario = usuario;
        this.url = url;
        this.ordem = ordem;
    }

    public String getUrl() {
        return url;
    }
}
