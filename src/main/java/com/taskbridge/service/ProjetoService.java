package com.taskbridge.service;

import com.taskbridge.dao.ProjetoBuscaDao;
import com.taskbridge.dao.ProjetoBuscaDao.FiltroProjetos;
import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.model.enums.PropostaStatus;
import com.taskbridge.model.enums.TipoNotificacao;
import com.taskbridge.model.enums.TipoRecurso;
import com.taskbridge.model.enums.UsuarioStatus;
import com.taskbridge.model.EscopoProjeto;
import com.taskbridge.model.FaixaValor;
import com.taskbridge.model.Projeto;
import com.taskbridge.model.Proposta;
import com.taskbridge.model.Usuario;
import com.taskbridge.dto.request.ProjetoRequest;
import com.taskbridge.dto.request.RequisitoRequest;
import com.taskbridge.dto.response.PaginaResponse;
import com.taskbridge.dto.response.ProjetoResponse;
import com.taskbridge.dto.response.ProjetoResumoResponse;
import com.taskbridge.exception.AcessoNegadoException;
import com.taskbridge.exception.RecursoNaoEncontradoException;
import com.taskbridge.exception.RegraNegocioException;
import com.taskbridge.repository.ProjetoRepository;
import com.taskbridge.repository.PropostaRepository;
import com.taskbridge.repository.UsuarioRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final ProjetoBuscaDao projetoBuscaDao;
    private final PropostaRepository propostaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaService categoriaService;
    private final UsuarioService usuarioService;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public ProjetoService(ProjetoRepository projetoRepository, ProjetoBuscaDao projetoBuscaDao,
                          PropostaRepository propostaRepository, UsuarioRepository usuarioRepository,
                          CategoriaService categoriaService, UsuarioService usuarioService,
                          NotificacaoService notificacoes, AuditoriaService auditoria, Clock clock) {
        this.projetoRepository = projetoRepository;
        this.projetoBuscaDao = projetoBuscaDao;
        this.propostaRepository = propostaRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaService = categoriaService;
        this.usuarioService = usuarioService;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ProjetoResumoResponse> listarAbertos(FiltroProjetos filtro, Pageable pageable) {
        return PaginaResponse.de(projetoBuscaDao.buscarAbertos(filtro, pageable), ProjetoResumoResponse::de);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ProjetoResumoResponse> listarDoCliente(Long clienteId, Pageable pageable) {
        return PaginaResponse.de(projetoRepository.findByClienteId(clienteId, pageable), ProjetoResumoResponse::de);
    }

    @Transactional(readOnly = true)
    public ProjetoResponse buscar(Long projetoId, Long visitanteId) {
        return ProjetoResponse.de(carregarVisivel(projetoId, visitanteId), visitanteId);
    }

    @Transactional
    public ProjetoResponse criar(Long clienteId, ProjetoRequest requisicao) {
        Projeto projeto = new Projeto(usuarioService.buscarAtivo(clienteId), montarEscopo(requisicao));
        projetoRepository.save(projeto);
        return ProjetoResponse.de(projeto, clienteId);
    }

    @Transactional
    public ProjetoResponse atualizar(Long projetoId, Long clienteId, ProjetoRequest requisicao) {
        Projeto projeto = carregarDoCliente(projetoId, clienteId);
        projeto.redefinirEscopo(montarEscopo(requisicao));
        return ProjetoResponse.de(projeto, clienteId);
    }

    @Transactional
    public ProjetoResponse publicar(Long projetoId, Long clienteId) {
        Projeto projeto = carregarDoCliente(projetoId, clienteId);
        projeto.publicar(clock.instant());
        auditoria.registrar(EventoAuditoria.PROJETO_PUBLICADO, clienteId, TipoRecurso.PROJETO, projetoId);
        return ProjetoResponse.de(projeto, clienteId);
    }

    /** Cancelar encerra a negociação: propostas pendentes são recusadas e os prestadores avisados. */
    @Transactional
    public ProjetoResponse cancelar(Long projetoId, Long clienteId) {
        Projeto projeto = carregarDoCliente(projetoId, clienteId);
        projeto.cancelar();
        recusarPendentes(projeto, clock.instant());
        auditoria.registrar(EventoAuditoria.PROJETO_CANCELADO, clienteId, TipoRecurso.PROJETO, projetoId);
        return ProjetoResponse.de(projeto, clienteId);
    }

    @Transactional
    public void excluir(Long projetoId, Long clienteId) {
        Projeto projeto = carregarDoCliente(projetoId, clienteId);
        if (!projeto.podeSerExcluido()) {
            throw new RegraNegocioException("Somente rascunhos podem ser excluídos. Projetos publicados podem ser cancelados.");
        }
        projetoRepository.delete(projeto);
    }

    /** "Solicitar proposta" a partir do matching: avisa o prestador, sem criar nada em nome dele. */
    @Transactional
    public void convidarPrestador(Long projetoId, Long clienteId, String nomeUsuario) {
        Projeto projeto = carregarDoCliente(projetoId, clienteId);
        if (!projeto.getStatus().aceitaPropostas()) {
            throw new RegraNegocioException("Publique o projeto antes de convidar profissionais.");
        }
        Usuario prestador = usuarioRepository
                .findByNomeUsuarioAndStatus(Usuario.normalizarNomeUsuario(nomeUsuario), UsuarioStatus.ATIVO)
                .filter(Usuario::isPrestador)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional"));
        // Idempotente: repetir o convite não gera outra notificação (evita usar o convite para spam).
        notificacoes.notificarUmaVez(prestador, TipoNotificacao.CONVITE_PROPOSTA, projeto.getTitulo(), "/projects/" + projetoId);
    }

    void recusarPendentes(Projeto projeto, Instant agora) {
        for (Proposta proposta : propostaRepository.findByProjetoIdAndStatus(projeto.getId(), PropostaStatus.PENDING)) {
            proposta.rejeitar(agora);
            notificacoes.notificar(proposta.getPrestador(), TipoNotificacao.PROPOSTA_RECUSADA,
                    projeto.getTitulo(), "/proposals");
        }
    }

    /** Projeto privado (rascunho/cancelado) de outra pessoa responde 404, não 403. */
    public Projeto carregarVisivel(Long projetoId, Long visitanteId) {
        return projetoRepository.findById(projetoId)
                .filter(projeto -> projeto.visivelPara(visitanteId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto"));
    }

    /** Regra 9: além de existir e ser visível, o projeto precisa ser do cliente autenticado. */
    public Projeto carregarDoCliente(Long projetoId, Long clienteId) {
        Projeto projeto = carregarVisivel(projetoId, clienteId);
        if (!projeto.pertenceA(clienteId)) {
            throw new AcessoNegadoException("Somente o dono do projeto pode realizar esta operação.");
        }
        return projeto;
    }

    private EscopoProjeto montarEscopo(ProjetoRequest requisicao) {
        EscopoProjeto.Builder escopo = EscopoProjeto.builder(requisicao.titulo(), requisicao.necessidade())
                .categoria(requisicao.categoriaId() == null ? null : categoriaService.buscar(requisicao.categoriaId()))
                .objetivo(requisicao.objetivo())
                .descricao(requisicao.descricao())
                .orcamento(FaixaValor.deOpcionais(requisicao.orcamentoMinimo(), requisicao.orcamentoMaximo()))
                .prazoDias(requisicao.prazoDias())
                .habilidades(requisicao.habilidades());
        List<RequisitoRequest> requisitos = requisicao.requisitos() == null ? List.of() : requisicao.requisitos();
        requisitos.forEach(item -> escopo.requisito(item.tipo(), item.descricao()));
        return escopo.build();
    }
}
