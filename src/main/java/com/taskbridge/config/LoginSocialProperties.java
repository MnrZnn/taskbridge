package com.taskbridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciais dos apps OAuth (Google Cloud Console / GitHub Developer Settings), sempre por variável de ambiente.
 * {@code urlBase} fixa o endereço público usado no redirect (ex.: https://seudominio.com); vazio = deduzido da requisição.
 */
@ConfigurationProperties("taskbridge.login-social")
public record LoginSocialProperties(Credencial google, Credencial github, String urlBase) {

    public record Credencial(String clientId, String clientSecret) {

        public boolean configurada() {
            return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
        }
    }
}
