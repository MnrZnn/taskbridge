package com.taskbridge.service;

import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.model.Usuario;
import com.taskbridge.dto.response.UsuarioResponse;
import com.taskbridge.storage.ArmazenamentoArquivos;
import com.taskbridge.storage.ProcessadorFotoPerfil;
import com.taskbridge.storage.ProcessadorFotoPerfil.FotoProcessada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FotoPerfilService {

    private final UsuarioService usuarioService;
    private final ProcessadorFotoPerfil processador;
    private final ArmazenamentoArquivos armazenamento;
    private final AuditoriaService auditoria;

    public FotoPerfilService(UsuarioService usuarioService, ProcessadorFotoPerfil processador,
                             ArmazenamentoArquivos armazenamento, AuditoriaService auditoria) {
        this.usuarioService = usuarioService;
        this.processador = processador;
        this.armazenamento = armazenamento;
        this.auditoria = auditoria;
    }

    @Transactional
    public UsuarioResponse atualizar(Long usuarioId, MultipartFile arquivo) {
        FotoProcessada foto = processador.processar(arquivo);
        Usuario usuario = usuarioService.buscarAtivo(usuarioId);
        String novoArquivo = armazenamento.salvar(foto.conteudo(), foto.extensao());
        String anterior = usuario.substituirFoto(novoArquivo).orElse(null);
        aposTransacao(anterior, novoArquivo);
        auditoria.registrar(EventoAuditoria.FOTO_ALTERADA, usuarioId);
        return UsuarioResponse.de(usuario);
    }

    @Transactional
    public void remover(Long usuarioId) {
        Usuario usuario = usuarioService.buscarAtivo(usuarioId);
        usuario.substituirFoto(null).ifPresent(anterior -> aposTransacao(anterior, null));
        auditoria.registrar(EventoAuditoria.FOTO_REMOVIDA, usuarioId);
    }

    /**
     * Disco não participa da transação do banco. Se a transação confirmar, apaga o arquivo antigo;
     * se for desfeita, apaga o novo — assim nunca fica foto órfã nem referência para arquivo apagado.
     */
    static void aposTransacao(String arquivoAntigo, String arquivoNovo, ArmazenamentoArquivos armazenamento) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                String descartado = status == STATUS_COMMITTED ? arquivoAntigo : arquivoNovo;
                if (descartado != null) {
                    armazenamento.remover(descartado);
                }
            }
        });
    }

    private void aposTransacao(String arquivoAntigo, String arquivoNovo) {
        aposTransacao(arquivoAntigo, arquivoNovo, armazenamento);
    }
}
