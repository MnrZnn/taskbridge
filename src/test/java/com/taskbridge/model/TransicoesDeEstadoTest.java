package com.taskbridge.model;

import com.taskbridge.model.enums.EntregaStatus;
import com.taskbridge.model.enums.PagamentoStatus;
import com.taskbridge.model.enums.ProjetoStatus;
import com.taskbridge.model.enums.PropostaStatus;
import com.taskbridge.exception.TransicaoEstadoInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransicoesDeEstadoTest {

    @ParameterizedTest
    @CsvSource({
            "DRAFT, PUBLISHED, true",
            "DRAFT, COMPLETED, false",
            "PUBLISHED, CONTRACTED, true",
            "IN_NEGOTIATION, CONTRACTED, true",
            "CONTRACTED, IN_PROGRESS, true",
            "CONTRACTED, CANCELLED, false",
            "IN_PROGRESS, COMPLETED, true",
            "COMPLETED, DRAFT, false",
            "CANCELLED, PUBLISHED, false"
    })
    void projeto(ProjetoStatus origem, ProjetoStatus destino, boolean permitido) {
        assertThat(origem.permiteTransicaoPara(destino)).isEqualTo(permitido);
    }

    @ParameterizedTest
    @EnumSource(value = PropostaStatus.class, names = {"ACCEPTED", "REJECTED", "CANCELLED"})
    void propostaRespondidaEFinal(PropostaStatus finalizada) {
        for (PropostaStatus destino : PropostaStatus.values()) {
            assertThat(finalizada.permiteTransicaoPara(destino)).isFalse();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, APPROVED, false",
            "PENDING, SUBMITTED, true",
            "SUBMITTED, APPROVED, true",
            "UNDER_REVIEW, APPROVED, true",
            "REVISION_REQUIRED, APPROVED, false",
            "APPROVED, PENDING, false",
            "APPROVED, REVISION_REQUIRED, false"
    })
    void entrega(EntregaStatus origem, EntregaStatus destino, boolean permitido) {
        assertThat(origem.permiteTransicaoPara(destino)).isEqualTo(permitido);
    }

    @Test
    void pagamentoPagoNaoPodeSerExpiradoNemCancelado() {
        assertThatThrownBy(() -> PagamentoStatus.PAID.transicionarPara(PagamentoStatus.EXPIRED))
                .isInstanceOf(TransicaoEstadoInvalidaException.class)
                .hasMessageContaining("Pago");
        assertThat(PagamentoStatus.PAID.permiteTransicaoPara(PagamentoStatus.CANCELLED)).isFalse();
    }
}
