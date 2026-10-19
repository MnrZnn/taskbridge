package com.taskbridge.storage;

import com.taskbridge.exception.ArquivoInvalidoException;
import com.taskbridge.storage.ProcessadorFotoPerfil.FotoProcessada;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessadorFotoPerfilTest {

    private final ProcessadorFotoPerfil processador = new ProcessadorFotoPerfil();

    @Test
    void imagemValidaEhRecortadaERecodificada() throws IOException {
        byte[] png = imagem(900, 600, "png");

        FotoProcessada foto = processador.processar(new MockMultipartFile("arquivo", "Minha Foto.PNG", "image/png", png));

        BufferedImage resultado = ImageIO.read(new ByteArrayInputStream(foto.conteudo()));
        assertThat(foto.extensao()).isEqualTo("png");
        assertThat(resultado.getWidth()).isEqualTo(resultado.getHeight()).isEqualTo(ProcessadorFotoPerfil.LADO_SAIDA);
    }

    @Test
    void executavelComExtensaoDeImagemEhRecusado() {
        MockMultipartFile falso = new MockMultipartFile("arquivo", "foto.png", "image/png", "MZ\u0090 executavel".getBytes());

        assertThatThrownBy(() -> processador.processar(falso))
                .isInstanceOf(ArquivoInvalidoException.class)
                .hasMessageContaining("não é uma imagem");
    }

    @Test
    void extensoesPerigosasSaoRecusadas() {
        for (String nome : new String[]{"shell.php", "pagina.jsp", "programa.exe", "script.sh", "sem-extensao"}) {
            MockMultipartFile arquivo = new MockMultipartFile("arquivo", nome, "image/png", new byte[]{1, 2, 3});
            assertThatThrownBy(() -> processador.processar(arquivo)).isInstanceOf(ArquivoInvalidoException.class);
        }
    }

    @Test
    void conteudoPrecisaCorresponderAExtensao() throws IOException {
        MockMultipartFile png = new MockMultipartFile("arquivo", "foto.jpg", "image/jpeg", imagem(10, 10, "png"));

        assertThatThrownBy(() -> processador.processar(png)).hasMessageContaining("não corresponde");
    }

    @Test
    void contentTypeDivergenteEhRecusado() throws IOException {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "foto.png", "text/html", imagem(10, 10, "png"));

        assertThatThrownBy(() -> processador.processar(arquivo)).isInstanceOf(ArquivoInvalidoException.class);
    }

    @Test
    void dimensoesExageradasSaoRecusadasAntesDeDecodificar() throws IOException {
        MockMultipartFile enorme = new MockMultipartFile("arquivo", "foto.png", "image/png", imagem(5000, 1, "png"));

        assertThatThrownBy(() -> processador.processar(enorme)).hasMessageContaining("pixels");
    }

    @Test
    void arquivoMaiorQueOLimiteEhRecusado() {
        MockMultipartFile grande = new MockMultipartFile("arquivo", "foto.png", "image/png",
                new byte[(int) ProcessadorFotoPerfil.TAMANHO_MAXIMO_BYTES + 1]);

        assertThatThrownBy(() -> processador.processar(grande)).hasMessageContaining("2 MB");
    }

    private static byte[] imagem(int largura, int altura, String formato) throws IOException {
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, formato, saida);
        return saida.toByteArray();
    }
}
