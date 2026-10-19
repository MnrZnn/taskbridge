package com.taskbridge.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/**
 * Monta o payload "copia e cola" no formato EMV-MPM do BR Code (Manual de Padrões para Iniciação do Pix).
 * Builder porque há campos obrigatórios, opcionais e limites de tamanho diferentes por campo,
 * e o CRC16 só pode ser calculado depois de tudo montado.
 */
public final class BrCodePix {

    private static final String GUI_PIX = "br.gov.bcb.pix";

    private BrCodePix() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String chave;
        private BigDecimal valor;
        private String nomeRecebedor;
        private String cidade;
        private String txid = "***";
        private String descricao;

        private Builder() {
        }

        public Builder chave(String chave) {
            this.chave = chave;
            return this;
        }

        public Builder valor(BigDecimal valor) {
            this.valor = valor;
            return this;
        }

        public Builder nomeRecebedor(String nomeRecebedor) {
            this.nomeRecebedor = nomeRecebedor;
            return this;
        }

        public Builder cidade(String cidade) {
            this.cidade = cidade;
            return this;
        }

        public Builder txid(String txid) {
            this.txid = txid;
            return this;
        }

        public Builder descricao(String descricao) {
            this.descricao = descricao;
            return this;
        }

        public String build() {
            Objects.requireNonNull(chave, "chave");
            Objects.requireNonNull(nomeRecebedor, "nomeRecebedor");
            Objects.requireNonNull(cidade, "cidade");

            StringBuilder payload = new StringBuilder()
                    .append(campo("00", "01"))
                    .append(campo("01", "12"))
                    .append(campo("26", contaRecebedor()))
                    .append(campo("52", "0000"))
                    .append(campo("53", "986"));
            if (valor != null) {
                payload.append(campo("54", valor.setScale(2, RoundingMode.HALF_UP).toPlainString()));
            }
            payload.append(campo("58", "BR"))
                    .append(campo("59", ascii(nomeRecebedor, 25)))
                    .append(campo("60", ascii(cidade, 15)))
                    .append(campo("62", campo("05", txid)))
                    .append("6304");
            return payload + crc16(payload.toString());
        }

        private String contaRecebedor() {
            String conta = campo("00", GUI_PIX) + campo("01", chave);
            if (descricao != null && !descricao.isBlank()) {
                conta += campo("02", ascii(descricao, 40));
            }
            return conta;
        }
    }

    static String campo(String id, String valor) {
        if (valor.length() > 99) {
            throw new IllegalArgumentException("Campo EMV " + id + " excede 99 caracteres");
        }
        return id + String.format("%02d", valor.length()) + valor;
    }

    /** O BR Code só aceita ASCII em nome e cidade; acentos são removidos. */
    static String ascii(String texto, int limite) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 .\\-]", "")
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT)
                .strip();
        return semAcento.length() <= limite ? semAcento : semAcento.substring(0, limite);
    }

    /** CRC16-CCITT-FALSE (polinômio 0x1021, valor inicial 0xFFFF), exigido pelo campo 63. */
    static String crc16(String dados) {
        int crc = 0xFFFF;
        for (byte b : dados.getBytes(StandardCharsets.US_ASCII)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
