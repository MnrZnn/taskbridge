package com.taskbridge.security;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * O campo "email" do perfil do GitHub pode vir vazio ou não verificado. Este serviço busca na API
 * /user/emails o e-mail primário **verificado** e o acrescenta aos atributos — só ele pode ser usado
 * para vincular a uma conta existente.
 */
@Component
public class GithubUsuarioService extends DefaultOAuth2UserService {

    static final String EMAIL_VERIFICADO = "taskbridge_email_verificado";

    private final RestClient github;

    public GithubUsuarioService() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.github = RestClient.builder().baseUrl("https://api.github.com").requestFactory(fabrica).build();
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest requisicao) {
        OAuth2User usuario = super.loadUser(requisicao);
        if (!"github".equals(requisicao.getClientRegistration().getRegistrationId())) {
            return usuario;
        }
        Map<String, Object> atributos = new HashMap<>(usuario.getAttributes());
        emailPrimarioVerificado(requisicao.getAccessToken().getTokenValue())
                .ifPresent(email -> atributos.put(EMAIL_VERIFICADO, email));
        return new DefaultOAuth2User(usuario.getAuthorities(), atributos, "id");
    }

    private Optional<String> emailPrimarioVerificado(String token) {
        List<Map<String, Object>> emails = github.get().uri("/user/emails")
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (emails == null) {
            return Optional.empty();
        }
        return emails.stream()
                .filter(email -> Boolean.TRUE.equals(email.get("primary")) && Boolean.TRUE.equals(email.get("verified")))
                .map(email -> String.valueOf(email.get("email")))
                .findFirst();
    }
}
