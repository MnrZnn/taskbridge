package com.taskbridge.pagamento;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/** O QR Code é derivado do payload a cada consulta; não há imagem guardada no banco. */
@Component
public class GeradorQrCode {

    private static final int LADO_PIXELS = 280;

    public String comoDataUri(String conteudo) {
        try (ByteArrayOutputStream png = new ByteArrayOutputStream()) {
            BitMatrix matriz = new QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, LADO_PIXELS, LADO_PIXELS,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
            MatrixToImageWriter.writeToStream(matriz, "PNG", png);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Falha ao gerar QR Code", e);
        }
    }
}
