package com.taskbridge.model;

import com.taskbridge.model.enums.TipoConta;
import com.taskbridge.model.enums.UsuarioStatus;
import com.taskbridge.exception.RegraNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nomeCompleto;

    @Column(nullable = false, length = 30, unique = true)
    private String nomeUsuario;

    @Column(nullable = false, length = 160, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoConta tipoConta;

    @Column(length = 1000)
    private String biografia;

    /** Apenas o nome do arquivo gerado pelo storage; nunca o nome enviado pelo usuário. */
    @Column(length = 80)
    private String fotoPerfil;

    @Column(length = 80)
    private String localizacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UsuarioStatus status;

    @Column(nullable = false)
    private int tentativasLoginFalhas;

    private Instant bloqueadoAte;

    @Column(nullable = false, length = 20)
    private String termosVersao;

    @Column(nullable = false)
    private Instant termosAceitosEm;

    /** Falso em contas criadas por login social até o usuário definir uma senha própria. */
    @Column(nullable = false)
    private boolean possuiSenha = true;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<LinkPerfil> links = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant dataCadastro;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant dataAtualizacao;

    protected Usuario() {
    }

    public Usuario(String nomeCompleto, String nomeUsuario, String email, String senhaHash,
                   TipoConta tipoConta, AceiteTermos aceite) {
        this.nomeCompleto = nomeCompleto.strip();
        this.nomeUsuario = normalizarNomeUsuario(nomeUsuario);
        this.email = normalizarEmail(email);
        this.senhaHash = Objects.requireNonNull(senhaHash);
        this.tipoConta = Objects.requireNonNull(tipoConta);
        this.termosVersao = aceite.versao();
        this.termosAceitosEm = aceite.aceitoEm();
        this.status = UsuarioStatus.ATIVO;
    }

    /**
     * Conta criada por login social: recebe um hash inutilizável (ninguém sabe a senha) e fica marcada
     * como sem senha, para que as telas peçam "definir senha" em vez de "senha atual".
     */
    public static Usuario semSenha(String nomeCompleto, String nomeUsuario, String email, String hashInutilizavel,
                                   TipoConta tipoConta, AceiteTermos aceite) {
        Usuario usuario = new Usuario(nomeCompleto, nomeUsuario, email, hashInutilizavel, tipoConta, aceite);
        usuario.possuiSenha = false;
        return usuario;
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    public static String normalizarNomeUsuario(String nomeUsuario) {
        return nomeUsuario == null ? null : nomeUsuario.strip().toLowerCase(Locale.ROOT);
    }

    public boolean isPrestador() {
        return tipoConta == TipoConta.PRESTADOR;
    }

    public boolean isCliente() {
        return tipoConta == TipoConta.CLIENTE;
    }

    public boolean isAtivo() {
        return status == UsuarioStatus.ATIVO;
    }

    public boolean estaBloqueado(Instant agora) {
        return bloqueadoAte != null && agora.isBefore(bloqueadoAte);
    }

    /**
     * Após {@code limite} falhas seguidas a conta fica bloqueada por {@code bloqueio}.
     * O bloqueio é curto de propósito: bloqueio longo vira ferramenta de negação de serviço
     * contra a vítima; o rate limit por IP cuida do volume.
     */
    public void registrarFalhaLogin(Instant agora, int limite, Duration bloqueio) {
        tentativasLoginFalhas++;
        if (tentativasLoginFalhas >= limite) {
            bloqueadoAte = agora.plus(bloqueio);
            tentativasLoginFalhas = 0;
        }
    }

    public void registrarLoginBemSucedido() {
        tentativasLoginFalhas = 0;
        bloqueadoAte = null;
    }

    public void atualizarDadosPessoais(String nomeCompleto, String biografia, String localizacao) {
        this.nomeCompleto = nomeCompleto.strip();
        this.biografia = textoOpcional(biografia);
        this.localizacao = textoOpcional(localizacao);
    }

    public void alterarEmail(String novoEmail) {
        this.email = normalizarEmail(novoEmail);
    }

    public void alterarSenha(String novoHash) {
        this.senhaHash = Objects.requireNonNull(novoHash);
        this.possuiSenha = true;
    }

    /** Substitui todos os links do perfil, na ordem informada. URLs são validadas por {@link UrlPublica}. */
    public void redefinirLinks(List<String> urls) {
        List<String> validas = urls == null ? List.of() : urls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(UrlPublica::normalizar)
                .distinct()
                .toList();
        if (validas.size() > LinkPerfil.MAXIMO_POR_USUARIO) {
            throw new RegraNegocioException("Adicione no máximo " + LinkPerfil.MAXIMO_POR_USUARIO + " links.");
        }
        links.clear();
        for (String url : validas) {
            links.add(new LinkPerfil(this, url, links.size()));
        }
    }

    /** @return o arquivo anterior, para que o chamador o remova do storage. */
    public Optional<String> substituirFoto(String novoArquivo) {
        String anterior = fotoPerfil;
        this.fotoPerfil = novoArquivo;
        return Optional.ofNullable(anterior);
    }

    /**
     * Exclusão de conta (LGPD): remove dados pessoais mas mantém a linha, porque contratações e
     * pagamentos concluídos precisam continuar referenciando uma das partes (retenção por obrigação legal).
     */
    public void anonimizar(String hashInutilizavel) {
        this.nomeCompleto = "Usuário removido";
        this.nomeUsuario = "removido-" + id;
        this.email = "removido-" + id + "@anonimizado.invalid";
        this.senhaHash = hashInutilizavel;
        this.biografia = null;
        this.localizacao = null;
        this.fotoPerfil = null;
        this.bloqueadoAte = null;
        this.tentativasLoginFalhas = 0;
        this.possuiSenha = false;
        this.links.clear();
        this.status = UsuarioStatus.EXCLUIDO;
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    public Long getId() {
        return id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public TipoConta getTipoConta() {
        return tipoConta;
    }

    public String getBiografia() {
        return biografia;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public UsuarioStatus getStatus() {
        return status;
    }

    public Instant getBloqueadoAte() {
        return bloqueadoAte;
    }

    public String getTermosVersao() {
        return termosVersao;
    }

    public Instant getTermosAceitosEm() {
        return termosAceitosEm;
    }

    public boolean isPossuiSenha() {
        return possuiSenha;
    }

    public List<String> getLinks() {
        return links.stream().map(LinkPerfil::getUrl).toList();
    }

    public Instant getDataCadastro() {
        return dataCadastro;
    }

    public Instant getDataAtualizacao() {
        return dataAtualizacao;
    }

    public record AceiteTermos(String versao, Instant aceitoEm) {
    }
}
