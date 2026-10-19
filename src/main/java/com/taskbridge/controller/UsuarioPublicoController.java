package com.taskbridge.controller;

import com.taskbridge.dto.response.PerfilPublicoResponse;
import com.taskbridge.exception.RecursoNaoEncontradoException;
import com.taskbridge.service.PerfilPublicoService;
import com.taskbridge.storage.ArmazenamentoArquivos;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@Tag(name = "Perfis públicos")
public class UsuarioPublicoController {

    private final PerfilPublicoService perfilPublicoService;
    private final ArmazenamentoArquivos armazenamento;

    public UsuarioPublicoController(PerfilPublicoService perfilPublicoService, ArmazenamentoArquivos armazenamento) {
        this.perfilPublicoService = perfilPublicoService;
        this.armazenamento = armazenamento;
    }

    @GetMapping("/api/usuarios/{nomeUsuario}")
    public PerfilPublicoResponse perfil(@PathVariable String nomeUsuario) {
        return perfilPublicoService.buscar(nomeUsuario);
    }

    /**
     * O tipo de conteúdo é decidido por nós (pela extensão que o próprio storage gerou), nunca pelo arquivo.
     * Junto com X-Content-Type-Options: nosniff, o navegador não tem como interpretar a foto como outra coisa.
     */
    @GetMapping("/api/arquivos/fotos/{arquivo}")
    public ResponseEntity<Resource> foto(@PathVariable String arquivo) {
        Resource foto = armazenamento.carregar(arquivo).orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo"));
        MediaType tipo = arquivo.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(tipo)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(foto);
    }
}
