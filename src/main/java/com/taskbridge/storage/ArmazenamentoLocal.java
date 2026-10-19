package com.taskbridge.storage;

import com.taskbridge.config.ArmazenamentoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Grava em um diretório fora do classpath e fora da pasta de arquivos estáticos: nada que for
 * enviado pode ser servido como página ou executado pelo servidor. A leitura só aceita nomes no
 * formato que este próprio storage gera, o que fecha qualquer tentativa de path traversal.
 */
@Component
public class ArmazenamentoLocal implements ArmazenamentoArquivos {

    private static final Logger log = LoggerFactory.getLogger(ArmazenamentoLocal.class);
    private static final Pattern NOME_GERADO = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg)");
    private static final Set<String> EXTENSOES = Set.of("png", "jpg");

    private final Path raiz;

    public ArmazenamentoLocal(ArmazenamentoProperties propriedades) {
        this.raiz = propriedades.diretorio().toAbsolutePath().normalize();
        criarDiretorio();
    }

    private void criarDiretorio() {
        try {
            Files.createDirectories(raiz);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar o diretório de uploads", e);
        }
    }

    @Override
    public String salvar(byte[] conteudo, String extensao) {
        if (!EXTENSOES.contains(extensao)) {
            throw new IllegalArgumentException("Extensão não suportada pelo storage");
        }
        String nome = UUID.randomUUID() + "." + extensao;
        try {
            Files.write(raiz.resolve(nome), conteudo, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return nome;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar arquivo", e);
        }
    }

    @Override
    public Optional<Resource> carregar(String nome) {
        return caminhoSeguro(nome).filter(Files::isRegularFile).map(FileSystemResource::new);
    }

    @Override
    public void remover(String nome) {
        caminhoSeguro(nome).ifPresent(caminho -> {
            try {
                Files.deleteIfExists(caminho);
            } catch (IOException e) {
                log.warn("Não foi possível remover arquivo do storage: {}", nome);
            }
        });
    }

    private Optional<Path> caminhoSeguro(String nome) {
        if (nome == null || !NOME_GERADO.matcher(nome).matches()) {
            return Optional.empty();
        }
        Path caminho = raiz.resolve(nome).normalize();
        return caminho.startsWith(raiz) ? Optional.of(caminho) : Optional.empty();
    }
}
