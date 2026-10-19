package com.taskbridge.storage;

import com.taskbridge.exception.ArquivoInvalidoException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;

/**
 * Trata a foto de perfil como entrada não confiável:
 * <ol>
 *   <li>limita tamanho e confere extensão e Content-Type (que o cliente controla — só um primeiro filtro);</li>
 *   <li>confere a assinatura real do arquivo (magic bytes) e exige que bata com a extensão;</li>
 *   <li>lê as dimensões pelo cabeçalho antes de decodificar, barrando "bombas de descompressão";</li>
 *   <li>decodifica e <b>re-codifica</b> a imagem: o arquivo salvo é gerado por nós, sem EXIF (que pode ter
 *       GPS do usuário) e sem qualquer conteúdo extra embutido no original (arquivos poliglotas).</li>
 * </ol>
 */
@Component
public class ProcessadorFotoPerfil {

    static final long TAMANHO_MAXIMO_BYTES = 2L * 1024 * 1024;
    static final int DIMENSAO_MAXIMA_ENTRADA = 4096;
    static final int LADO_SAIDA = 400;

    public FotoProcessada processar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Selecione uma imagem.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new ArquivoInvalidoException("A imagem deve ter no máximo 2 MB.");
        }
        FormatoImagem pelaExtensao = FormatoImagem.porExtensao(extensao(arquivo.getOriginalFilename()))
                .orElseThrow(() -> new ArquivoInvalidoException("Envie uma imagem JPG ou PNG."));
        if (!pelaExtensao.aceitaContentType(arquivo.getContentType())) {
            throw new ArquivoInvalidoException("Tipo de arquivo não permitido.");
        }
        byte[] conteudo = lerBytes(arquivo);
        FormatoImagem real = FormatoImagem.porAssinatura(conteudo)
                .orElseThrow(() -> new ArquivoInvalidoException("O conteúdo do arquivo não é uma imagem JPG ou PNG."));
        if (real != pelaExtensao) {
            throw new ArquivoInvalidoException("O conteúdo do arquivo não corresponde à extensão.");
        }
        validarDimensoes(conteudo);
        BufferedImage imagem = decodificar(conteudo);
        return new FotoProcessada(codificar(recortarQuadrado(imagem, real), real), real.extensaoArmazenada);
    }

    private static String extensao(String nomeOriginal) {
        if (nomeOriginal == null || !nomeOriginal.contains(".")) {
            return "";
        }
        return nomeOriginal.substring(nomeOriginal.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static byte[] lerBytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler o arquivo enviado.");
        }
    }

    private static void validarDimensoes(byte[] conteudo) {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(conteudo))) {
            Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) {
                throw new ArquivoInvalidoException("Imagem inválida.");
            }
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                if (leitor.getWidth(0) > DIMENSAO_MAXIMA_ENTRADA || leitor.getHeight(0) > DIMENSAO_MAXIMA_ENTRADA) {
                    throw new ArquivoInvalidoException("A imagem deve ter no máximo " + DIMENSAO_MAXIMA_ENTRADA + " pixels de lado.");
                }
            } finally {
                leitor.dispose();
            }
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Imagem inválida.");
        }
    }

    private static BufferedImage decodificar(byte[] conteudo) {
        try {
            BufferedImage imagem = ImageIO.read(new ByteArrayInputStream(conteudo));
            if (imagem == null) {
                throw new ArquivoInvalidoException("Imagem inválida.");
            }
            return imagem;
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Imagem inválida.");
        }
    }

    private static BufferedImage recortarQuadrado(BufferedImage original, FormatoImagem formato) {
        int lado = Math.min(original.getWidth(), original.getHeight());
        int x = (original.getWidth() - lado) / 2;
        int y = (original.getHeight() - lado) / 2;
        int saida = Math.min(lado, LADO_SAIDA);
        BufferedImage destino = new BufferedImage(saida, saida, formato.tipoBufferedImage);
        Graphics2D g = destino.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(original, 0, 0, saida, saida, x, y, x + lado, y + lado, null);
        } finally {
            g.dispose();
        }
        return destino;
    }

    private static byte[] codificar(BufferedImage imagem, FormatoImagem formato) {
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            if (!ImageIO.write(imagem, formato.nomeImageIo, saida)) {
                throw new ArquivoInvalidoException("Não foi possível processar a imagem.");
            }
            return saida.toByteArray();
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível processar a imagem.");
        }
    }

    public record FotoProcessada(byte[] conteudo, String extensao) {
    }

    enum FormatoImagem {
        PNG("png", "png", "image/png", BufferedImage.TYPE_INT_ARGB, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}),
        JPEG("jpg", "jpeg", "image/jpeg", BufferedImage.TYPE_INT_RGB, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        private final String extensaoArmazenada;
        private final String nomeImageIo;
        private final String contentType;
        private final int tipoBufferedImage;
        private final byte[] assinatura;

        FormatoImagem(String extensaoArmazenada, String nomeImageIo, String contentType, int tipoBufferedImage, byte[] assinatura) {
            this.extensaoArmazenada = extensaoArmazenada;
            this.nomeImageIo = nomeImageIo;
            this.contentType = contentType;
            this.tipoBufferedImage = tipoBufferedImage;
            this.assinatura = assinatura;
        }

        static Optional<FormatoImagem> porExtensao(String extensao) {
            return switch (extensao) {
                case "png" -> Optional.of(PNG);
                case "jpg", "jpeg" -> Optional.of(JPEG);
                default -> Optional.empty();
            };
        }

        static Optional<FormatoImagem> porAssinatura(byte[] conteudo) {
            return Arrays.stream(values()).filter(formato -> formato.comecaCom(conteudo)).findFirst();
        }

        boolean aceitaContentType(String contentType) {
            return this.contentType.equalsIgnoreCase(contentType);
        }

        private boolean comecaCom(byte[] conteudo) {
            return conteudo.length >= assinatura.length
                    && Arrays.equals(Arrays.copyOf(conteudo, assinatura.length), assinatura);
        }
    }
}
