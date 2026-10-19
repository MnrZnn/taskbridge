package com.taskbridge.model;

import com.taskbridge.model.enums.ProvedorLogin;
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

/**
 * Liga uma conta a uma identidade no Google/GitHub. O vínculo é pelo {@code sujeito} (id estável do
 * usuário no provedor), nunca pelo e-mail: e-mail pode mudar ou ser reaproveitado por outra pessoa.
 */
@Entity
@Table(name = "identidade_externa")
public class IdentidadeExterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProvedorLogin provedor;

    @Column(nullable = false, length = 100)
    private String sujeito;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    protected IdentidadeExterna() {
    }

    public IdentidadeExterna(Usuario usuario, ProvedorLogin provedor, String sujeito, Instant agora) {
        this.usuario = usuario;
        this.provedor = provedor;
        this.sujeito = sujeito;
        this.criadaEm = agora;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public ProvedorLogin getProvedor() {
        return provedor;
    }
}
