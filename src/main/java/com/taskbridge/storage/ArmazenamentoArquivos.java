package com.taskbridge.storage;

import org.springframework.core.io.Resource;

import java.util.Optional;

/**
 * Fronteira entre a aplicação e onde os arquivos ficam. Hoje é o disco local
 * ({@link ArmazenamentoLocal}); um bucket S3/GCS seria outra implementação desta interface.
 */
public interface ArmazenamentoArquivos {

    /** @return nome gerado pelo storage — o nome original enviado pelo usuário nunca é usado. */
    String salvar(byte[] conteudo, String extensao);

    Optional<Resource> carregar(String nome);

    void remover(String nome);
}
