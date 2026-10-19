package com.taskbridge.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Limita o corpo JSON da API. Com Content-Length declarado, recusa antes de ler (413).
 * Sem ele (chunked — comum atrás de proxy HTTP/2), conta os bytes durante a leitura e interrompe
 * ao passar do limite. Uploads multipart têm limite próprio (spring.servlet.multipart.*).
 */
public class LimiteCorpoRequisicaoFilter extends OncePerRequestFilter {

    private static final Set<String> METODOS_COM_CORPO = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final long maximoBytes;
    private final EscritorRespostaErro escritorErro;

    public LimiteCorpoRequisicaoFilter(long maximoBytes, EscritorRespostaErro escritorErro) {
        this.maximoBytes = maximoBytes;
        this.escritorErro = escritorErro;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/")
                || !METODOS_COM_CORPO.contains(request.getMethod())
                || isMultipart(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > maximoBytes) {
            escritorErro.escrever(request, response, HttpStatus.CONTENT_TOO_LARGE, "Requisição grande demais.");
            return;
        }
        chain.doFilter(new RequisicaoLimitada(request, maximoBytes), response);
    }

    private static boolean isMultipart(HttpServletRequest request) {
        String tipo = request.getContentType();
        return tipo != null && tipo.toLowerCase().startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    /** Lançada durante a leitura; o handler global a traduz para 413. */
    public static class CorpoGrandeDemaisException extends IOException {

        public CorpoGrandeDemaisException() {
            super("Corpo da requisição excede o limite");
        }
    }

    private static final class RequisicaoLimitada extends HttpServletRequestWrapper {

        private final long maximoBytes;
        private ServletInputStream entrada;

        RequisicaoLimitada(HttpServletRequest request, long maximoBytes) {
            super(request);
            this.maximoBytes = maximoBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (entrada == null) {
                entrada = new EntradaLimitada(super.getInputStream(), maximoBytes);
            }
            return entrada;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String codificacao = getCharacterEncoding();
            Charset charset = codificacao == null ? StandardCharsets.UTF_8 : Charset.forName(codificacao);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }

    private static final class EntradaLimitada extends ServletInputStream {

        private final ServletInputStream original;
        private final long maximoBytes;
        private long lidos;

        EntradaLimitada(ServletInputStream original, long maximoBytes) {
            this.original = original;
            this.maximoBytes = maximoBytes;
        }

        @Override
        public int read() throws IOException {
            int valor = original.read();
            if (valor != -1) {
                contar(1);
            }
            return valor;
        }

        @Override
        public int read(byte[] destino, int inicio, int tamanho) throws IOException {
            int quantidade = original.read(destino, inicio, tamanho);
            if (quantidade > 0) {
                contar(quantidade);
            }
            return quantidade;
        }

        private void contar(int quantidade) throws CorpoGrandeDemaisException {
            lidos += quantidade;
            if (lidos > maximoBytes) {
                throw new CorpoGrandeDemaisException();
            }
        }

        @Override
        public boolean isFinished() {
            return original.isFinished();
        }

        @Override
        public boolean isReady() {
            return original.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            original.setReadListener(listener);
        }
    }
}
