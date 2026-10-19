package com.taskbridge.dto.response;

import com.taskbridge.model.enums.ModoPagamento;
import com.taskbridge.model.enums.PagamentoStatus;
import com.taskbridge.model.Pagamento;

import java.math.BigDecimal;
import java.time.Instant;

public record PagamentoResponse(
        Long id,
        Long contratacaoId,
        String projetoTitulo,
        BigDecimal valor,
        PagamentoStatus status,
        String statusRotulo,
        String txid,
        String pixCopiaECola,
        String qrCode,
        Instant criadoEm,
        Instant expiraEm,
        Instant pagoEm,
        ModoPagamento modo,
        boolean ambienteTeste) {

    /** O QR Code só é enviado enquanto a cobrança pode ser paga. */
    public static PagamentoResponse de(Pagamento pagamento, String qrCodeDataUri) {
        boolean emAberto = pagamento.getStatus().emAberto();
        return new PagamentoResponse(
                pagamento.getId(),
                pagamento.getContratacao().getId(),
                pagamento.getContratacao().getProjeto().getTitulo(),
                pagamento.getValor(),
                pagamento.getStatus(),
                pagamento.getStatus().rotulo(),
                pagamento.getTxid(),
                emAberto ? pagamento.getPayloadPix() : null,
                emAberto ? qrCodeDataUri : null,
                pagamento.getCriadoEm(),
                pagamento.getExpiraEm(),
                pagamento.getPagoEm(),
                pagamento.getModo(),
                pagamento.getModo() == ModoPagamento.MOCK);
    }
}
