package com.taskbridge.model;

import com.taskbridge.exception.RegraNegocioException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Valida URLs que o usuário publica no perfil. Só http/https com domínio real: bloqueia
 * {@code javascript:}, {@code data:} e afins (XSS ao clicar), URLs com usuário embutido
 * ({@code https://banco.com@golpe.com}, usado em phishing) e endereços locais.
 */
public final class UrlPublica {

    static final int TAMANHO_MAXIMO = 300;
    private static final Set<String> ESQUEMAS = Set.of("http", "https");

    private UrlPublica() {
    }

    /** Aceita "linkedin.com/in/fulano" e completa com https://. */
    public static String normalizar(String bruta) {
        String texto = bruta.strip();
        if (texto.length() > TAMANHO_MAXIMO) {
            throw invalida();
        }
        if (!texto.contains("://")) {
            texto = "https://" + texto;
        }
        URI uri;
        try {
            uri = new URI(texto).normalize();
        } catch (URISyntaxException e) {
            throw invalida();
        }
        String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost();
        if (!ESQUEMAS.contains(esquema) || host == null || !host.contains(".") || uri.getRawUserInfo() != null) {
            throw invalida();
        }
        return esquema + texto.substring(texto.indexOf("://"));
    }

    private static RegraNegocioException invalida() {
        return new RegraNegocioException("Link inválido. Use um endereço http(s) completo, como https://www.linkedin.com/in/seu-perfil.");
    }
}
