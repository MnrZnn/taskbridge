package com.taskbridge.service;

import com.taskbridge.model.DadosProfissionais;
import com.taskbridge.model.FaixaValor;
import com.taskbridge.model.PerfilProfissional;
import com.taskbridge.dto.request.PerfilProfissionalRequest;
import com.taskbridge.dto.response.PerfilProfissionalResponse;
import com.taskbridge.repository.PerfilProfissionalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PerfilProfissionalService {

    private final PerfilProfissionalRepository repository;
    private final UsuarioService usuarioService;

    public PerfilProfissionalService(PerfilProfissionalRepository repository, UsuarioService usuarioService) {
        this.repository = repository;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public Optional<PerfilProfissionalResponse> buscar(Long usuarioId) {
        return repository.findByUsuarioId(usuarioId).map(PerfilProfissionalResponse::de);
    }

    @Transactional
    public PerfilProfissionalResponse salvar(Long usuarioId, PerfilProfissionalRequest requisicao) {
        DadosProfissionais dados = new DadosProfissionais(
                requisicao.tituloProfissional(),
                requisicao.descricao(),
                requisicao.experienciaAnos(),
                requisicao.disponibilidade(),
                requisicao.modalidadeTrabalho(),
                new FaixaValor(requisicao.faixaPrecoMinima(), requisicao.faixaPrecoMaxima()),
                requisicao.habilidades());
        PerfilProfissional perfil = repository.findByUsuarioId(usuarioId)
                .map(existente -> {
                    existente.atualizar(dados);
                    return existente;
                })
                .orElseGet(() -> repository.save(new PerfilProfissional(usuarioService.buscarAtivo(usuarioId), dados)));
        return PerfilProfissionalResponse.de(perfil);
    }
}
