package com.taskbridge.service;

import com.taskbridge.model.DadosServico;
import com.taskbridge.model.Servico;
import com.taskbridge.dto.request.ServicoRequest;
import com.taskbridge.dto.response.PaginaResponse;
import com.taskbridge.dto.response.ServicoResponse;
import com.taskbridge.exception.AcessoNegadoException;
import com.taskbridge.exception.RecursoNaoEncontradoException;
import com.taskbridge.repository.ServicoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoService {

    private final ServicoRepository repository;
    private final CategoriaService categoriaService;
    private final UsuarioService usuarioService;

    public ServicoService(ServicoRepository repository, CategoriaService categoriaService, UsuarioService usuarioService) {
        this.repository = repository;
        this.categoriaService = categoriaService;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ServicoResponse> listarAtivos(Long categoriaId, Pageable pageable) {
        return PaginaResponse.de(repository.buscarAtivos(categoriaId, pageable), ServicoResponse::de);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ServicoResponse> listarDoPrestador(Long prestadorId, Pageable pageable) {
        return PaginaResponse.de(repository.findByPrestadorId(prestadorId, pageable), ServicoResponse::de);
    }

    @Transactional(readOnly = true)
    public ServicoResponse buscar(Long servicoId, Long visitanteId) {
        return ServicoResponse.de(buscarVisivel(servicoId, visitanteId));
    }

    @Transactional
    public ServicoResponse criar(Long prestadorId, ServicoRequest requisicao) {
        Servico servico = new Servico(usuarioService.buscarAtivo(prestadorId), dados(requisicao));
        return ServicoResponse.de(repository.save(servico));
    }

    @Transactional
    public ServicoResponse atualizar(Long servicoId, Long prestadorId, ServicoRequest requisicao) {
        Servico servico = buscarDoPrestador(servicoId, prestadorId);
        servico.atualizar(dados(requisicao));
        return ServicoResponse.de(servico);
    }

    @Transactional
    public ServicoResponse pausar(Long servicoId, Long prestadorId) {
        Servico servico = buscarDoPrestador(servicoId, prestadorId);
        servico.pausar();
        return ServicoResponse.de(servico);
    }

    @Transactional
    public ServicoResponse reativar(Long servicoId, Long prestadorId) {
        Servico servico = buscarDoPrestador(servicoId, prestadorId);
        servico.reativar();
        return ServicoResponse.de(servico);
    }

    @Transactional
    public void excluir(Long servicoId, Long prestadorId) {
        repository.delete(buscarDoPrestador(servicoId, prestadorId));
    }

    /** Serviço pausado só é visível para o dono; para os demais, simplesmente não existe. */
    private Servico buscarVisivel(Long servicoId, Long visitanteId) {
        return repository.findById(servicoId)
                .filter(servico -> servico.visivelPara(visitanteId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço"));
    }

    /** Regra 9: o id na URL não basta, o serviço precisa ser do usuário autenticado. */
    private Servico buscarDoPrestador(Long servicoId, Long prestadorId) {
        Servico servico = buscarVisivel(servicoId, prestadorId);
        if (!servico.pertenceA(prestadorId)) {
            throw new AcessoNegadoException("Você só pode alterar os seus próprios serviços.");
        }
        return servico;
    }

    private DadosServico dados(ServicoRequest requisicao) {
        return new DadosServico(categoriaService.buscar(requisicao.categoriaId()), requisicao.titulo(),
                requisicao.descricao(), requisicao.precoBase(), requisicao.prazoDias());
    }
}
