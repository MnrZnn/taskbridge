package com.taskbridge.security;

import com.taskbridge.config.LoginSocialProperties;
import com.taskbridge.config.LoginSocialProperties.Credencial;
import com.taskbridge.model.enums.ProvedorLogin;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Monta os registros OAuth só para os provedores com credenciais configuradas. Sem nenhum, o login social
 * simplesmente não existe (os botões nem aparecem) — a aplicação sobe normalmente sem as chaves.
 */
@Component
public class ProvedoresLoginSocial {

    private final List<ClientRegistration> registros = new ArrayList<>();

    public ProvedoresLoginSocial(LoginSocialProperties propriedades) {
        String redirect = propriedades.urlBase() == null || propriedades.urlBase().isBlank()
                ? null
                : propriedades.urlBase().replaceAll("/+$", "") + "/login/oauth2/code/{registrationId}";
        adicionar(CommonOAuth2Provider.GOOGLE, ProvedorLogin.GOOGLE, propriedades.google(), redirect);
        adicionar(CommonOAuth2Provider.GITHUB, ProvedorLogin.GITHUB, propriedades.github(), redirect);
    }

    private void adicionar(CommonOAuth2Provider tipo, ProvedorLogin provedor, Credencial credencial, String redirect) {
        if (credencial == null || !credencial.configurada()) {
            return;
        }
        ClientRegistration.Builder builder = tipo.getBuilder(provedor.idRegistro())
                .clientId(credencial.clientId())
                .clientSecret(credencial.clientSecret());
        if (provedor == ProvedorLogin.GITHUB) {
            // user:email permite ler os e-mails verificados; sem isso não há como vincular contas com segurança.
            builder.scope("read:user", "user:email");
        }
        if (redirect != null) {
            builder.redirectUri(redirect);
        }
        registros.add(builder.build());
    }

    public List<ProvedorLogin> ativos() {
        return registros.stream().map(registro -> ProvedorLogin.doIdRegistro(registro.getRegistrationId())).toList();
    }

    public Optional<ClientRegistrationRepository> repositorio() {
        return registros.isEmpty() ? Optional.empty() : Optional.of(new InMemoryClientRegistrationRepository(registros));
    }
}
