package com.taskbridge.security;

import com.taskbridge.security.LimiteCorpoRequisicaoFilter.CorpoGrandeDemaisException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class LimiteCorpoRequisicaoFilterTest {

    private final LimiteCorpoRequisicaoFilter filtro = new LimiteCorpoRequisicaoFilter(1024, mock(EscritorRespostaErro.class));

    /** Simula corpo chunked (sem Content-Length), comum atrás de proxy HTTP/2. */
    private static MockHttpServletRequest semTamanhoDeclarado(int bytes) {
        MockHttpServletRequest requisicao = new MockHttpServletRequest("POST", "/api/projetos") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        requisicao.setContentType("application/json");
        requisicao.setContent(new byte[bytes]);
        return requisicao;
    }

    @Test
    void corpoChunkedAcimaDoLimiteEhInterrompidoDuranteALeitura() {
        MockHttpServletRequest requisicao = semTamanhoDeclarado(5000);

        assertThatThrownBy(() -> filtro.doFilter(requisicao, new MockHttpServletResponse(),
                (req, res) -> req.getInputStream().readAllBytes()))
                .isInstanceOf(CorpoGrandeDemaisException.class);
    }

    @Test
    void corpoChunkedDentroDoLimitePassa() throws Exception {
        MockHttpServletRequest requisicao = semTamanhoDeclarado(500);
        int[] lidos = new int[1];

        filtro.doFilter(requisicao, new MockHttpServletResponse(), (req, res) -> lidos[0] = req.getInputStream().readAllBytes().length);

        assertThat(lidos[0]).isEqualTo(500);
    }
}
