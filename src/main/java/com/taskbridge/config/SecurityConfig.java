package com.taskbridge.config;

import com.taskbridge.security.AuditoriaLogoutHandler;
import com.taskbridge.security.EscritorRespostaErro;
import com.taskbridge.security.GithubUsuarioService;
import com.taskbridge.security.LoginSocialHandler;
import com.taskbridge.security.ProvedoresLoginSocial;
import com.taskbridge.security.LimitadorRequisicoes;
import com.taskbridge.security.LimiteCorpoRequisicaoFilter;
import com.taskbridge.security.RateLimitFilter;
import com.taskbridge.security.RespostasSeguranca;
import com.taskbridge.security.UsuarioDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * Autenticação por sessão (cookie HttpOnly) com CSRF ativo. O front-end é servido pela mesma origem,
 * então não há token em localStorage para um XSS roubar. Ver docs/ARQUITETURA.md §8.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self' data: blob:",
            "font-src 'self'",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    private static final String[] GET_PUBLICOS = {
            "/api/auth/csrf", "/api/config/publica", "/api/categorias",
            "/api/servicos", "/api/servicos/{id}", "/api/projetos", "/api/projetos/{id}",
            "/api/usuarios/{nomeUsuario}", "/api/arquivos/fotos/{arquivo}"
    };

    /** Casariam com os padrões "{id}" acima, mas dependem do usuário logado. */
    private static final String[] GET_AUTENTICADOS = {
            "/api/servicos/meus", "/api/projetos/meus", "/api/projetos/compativeis"
    };

    @Bean
    SecurityFilterChain filtros(HttpSecurity http,
                                RespostasSeguranca respostas,
                                AuditoriaLogoutHandler auditoriaLogout,
                                CsrfTokenRepository csrfTokenRepository,
                                SecurityContextRepository securityContextRepository,
                                RateLimitFilter rateLimitFilter,
                                @Value("${server.servlet.session.cookie.name}") String cookieSessao,
                                ProvedoresLoginSocial provedoresSociais,
                                LoginSocialHandler loginSocial,
                                GithubUsuarioService githubUsuarios) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .cors(Customizer.withDefaults())
                .securityContext(contexto -> contexto.securityContextRepository(securityContextRepository))
                .sessionManagement(sessao -> sessao.sessionFixation(fixacao -> fixacao.changeSessionId()))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, GET_AUTENTICADOS).authenticated()
                        .requestMatchers(HttpMethod.GET, GET_PUBLICOS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro").permitAll()
                        // Etapa final do login social: protegida pelos dados guardados na sessão, não por login.
                        .requestMatchers("/api/auth/cadastro-social").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/**", "/actuator/**").authenticated()
                        // Páginas e arquivos estáticos não contêm dados: tudo vem da API, que é protegida acima.
                        .anyRequest().permitAll())
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(respostas)
                        .accessDeniedHandler(respostas))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .addLogoutHandler(auditoriaLogout)
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                        .deleteCookies(cookieSessao))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .frameOptions(frame -> frame.deny())
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                "camera=(), microphone=(), geolocation=(), payment=()")))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(cache -> cache.disable())
                .addFilterAfter(rateLimitFilter, SecurityContextHolderFilter.class);

        // Login com Google/GitHub só é ligado para os provedores com credenciais configuradas.
        // O entry point JSON acima continua valendo para a API (não redireciona chamadas /api para o login).
        provedoresSociais.repositorio().ifPresent(repositorio -> http.oauth2Login(oauth -> oauth
                .clientRegistrationRepository(repositorio)
                .loginPage("/login")
                .userInfoEndpoint(info -> info.userService(githubUsuarios))
                .successHandler(loginSocial)
                .failureHandler(loginSocial)));
        return http.build();
    }

    /**
     * Token CSRF guardado na sessão (servidor), não em cookie: o front-end o obtém em GET /api/auth/csrf
     * e devolve no header X-XSRF-TOKEN. Um site de terceiros não consegue ler essa resposta (mesma origem),
     * então não consegue forjar o header. Sem cookie extra, funciona também atrás do Firebase Hosting,
     * que só repassa ao backend o cookie "__session".
     */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repositorio = new HttpSessionCsrfTokenRepository();
        repositorio.setHeaderName("X-XSRF-TOKEN");
        return repositorio;
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
        CsrfAuthenticationStrategy novoTokenCsrf = new CsrfAuthenticationStrategy(csrfTokenRepository);
        novoTokenCsrf.setRequestHandler(new CsrfTokenRequestAttributeHandler());
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                novoTokenCsrf));
    }

    @Bean
    PasswordEncoder passwordEncoder(SegurancaProperties seguranca) {
        return new BCryptPasswordEncoder(seguranca.bcryptForca());
    }

    @Bean
    AuthenticationManager authenticationManager(UsuarioDetailsService usuarios, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provedor = new DaoAuthenticationProvider(usuarios);
        provedor.setPasswordEncoder(passwordEncoder);
        // Por padrão o bloqueio é checado ANTES do BCrypt: conta bloqueada responderia mais rápido e isso
        // revelaria que ela existe. Checando depois, toda tentativa custa o mesmo tempo.
        provedor.setPreAuthenticationChecks(usuario -> { });
        provedor.setPostAuthenticationChecks(new AccountStatusUserDetailsChecker());
        return new ProviderManager(provedor);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SegurancaProperties seguranca) {
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        if (seguranca.corsOrigens().isEmpty()) {
            return fonte; // produção: só a própria origem, nenhuma exceção de CORS
        }
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(seguranca.corsOrigens());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(Duration.ofHours(1));
        fonte.registerCorsConfiguration("/api/**", cors);
        return fonte;
    }

    @Bean
    RateLimitFilter rateLimitFilter(RateLimitProperties limites, EscritorRespostaErro escritorErro) {
        return new RateLimitFilter(new LimitadorRequisicoes(100_000, Duration.ofHours(2)), limites, escritorErro);
    }

    /** Já está dentro da cadeia do Spring Security; sem isto o Boot o registraria uma segunda vez. */
    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filtro) {
        FilterRegistrationBean<RateLimitFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    FilterRegistrationBean<LimiteCorpoRequisicaoFilter> limiteCorpoRequisicao(SegurancaProperties seguranca,
                                                                             EscritorRespostaErro escritorErro) {
        LimiteCorpoRequisicaoFilter filtro =
                new LimiteCorpoRequisicaoFilter(seguranca.maxCorpoJson().toBytes(), escritorErro);
        FilterRegistrationBean<LimiteCorpoRequisicaoFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registro.addUrlPatterns("/api/*");
        return registro;
    }
}
