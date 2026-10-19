package com.taskbridge.security;

import com.taskbridge.config.RateLimitProperties;
import com.taskbridge.config.RateLimitProperties.Limite;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Limites da camada de aplicação: login e cadastro por IP (quem ataca ainda não tem conta),
 * upload e API por usuário autenticado (ou IP, se anônimo). Não substitui proteção de rede contra
 * DDoS volumétrico — isso é papel de CDN/WAF/proxy, antes de a requisição chegar aqui.
 * <p>
 * O IP vem de {@code getRemoteAddr()}. Em produção, atrás do proxy, o Tomcat já o substitui pelo
 * IP real a partir de X-Forwarded-For ({@code server.forward-headers-strategy=native}). Ler o
 * header diretamente aqui permitiria que qualquer cliente escolhesse o próprio "IP".
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final LimitadorRequisicoes limitador;
    private final RateLimitProperties limites;
    private final EscritorRespostaErro escritorErro;

    public RateLimitFilter(LimitadorRequisicoes limitador, RateLimitProperties limites, EscritorRespostaErro escritorErro) {
        this.limitador = limitador;
        this.limites = limites;
        this.escritorErro = escritorErro;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Politica politica = Politica.para(request);
        String chave = politica.name() + ":" + identificador(request, politica);
        ConsumptionProbe consumo = limitador.consumir(chave, politica.limite(limites));
        if (consumo.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(consumo.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }
        long segundos = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(consumo.getNanosToWaitForRefill()));
        log.warn("Rate limit excedido: politica={} uri={}", politica, request.getRequestURI());
        response.setHeader("Retry-After", String.valueOf(segundos));
        escritorErro.escrever(request, response, HttpStatus.TOO_MANY_REQUESTS,
                "Muitas requisições. Tente novamente em " + segundos + " segundo(s).");
    }

    private static String identificador(HttpServletRequest request, Politica politica) {
        if (politica.porIp) {
            return request.getRemoteAddr();
        }
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return "u" + usuario.getId();
        }
        return request.getRemoteAddr();
    }

    enum Politica {
        LOGIN(true),
        REGISTRO(true),
        UPLOAD(false),
        API(false);

        private final boolean porIp;

        Politica(boolean porIp) {
            this.porIp = porIp;
        }

        static Politica para(HttpServletRequest request) {
            if (!"POST".equals(request.getMethod())) {
                return API;
            }
            return switch (request.getRequestURI()) {
                case "/api/auth/login" -> LOGIN;
                case "/api/auth/registro" -> REGISTRO;
                case "/api/me/foto" -> UPLOAD;
                default -> API;
            };
        }

        Limite limite(RateLimitProperties limites) {
            return switch (this) {
                case LOGIN -> limites.login();
                case REGISTRO -> limites.registro();
                case UPLOAD -> limites.upload();
                case API -> limites.api();
            };
        }
    }
}
