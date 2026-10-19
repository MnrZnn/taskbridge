package com.taskbridge.service;

import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.model.enums.TipoNotificacao;
import com.taskbridge.model.enums.TipoRecurso;
import com.taskbridge.model.Contratacao;
import com.taskbridge.model.Projeto;
import com.taskbridge.model.Proposta;
import com.taskbridge.dto.request.PropostaRequest;
import com.taskbridge.dto.response.ContratacaoResponse;
import com.taskbridge.dto.response.PaginaResponse;
import com.taskbridge.dto.response.PropostaResponse;
import com.taskbridge.exception.AcessoNegadoException;
import com.taskbridge.exception.ConflitoException;
import com.taskbridge.exception.RecursoNaoEncontradoException;
import com.taskbridge.repository.ContratacaoRepository;
import com.taskbridge.repository.ProjetoRepository;
import com.taskbridge.repository.PropostaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class PropostaService {

    private final PropostaRepository propostaRepository;
    private final ProjetoRepository projetoRepository;
    private final ContratacaoRepository contratacaoRepository;
    private final ProjetoService projetoService;
    private final UsuarioService usuarioService;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public PropostaService(PropostaRepository propostaRepository, ProjetoRepository projetoRepository,
                           ContratacaoRepository contratacaoRepository, ProjetoService projetoService,
                           UsuarioService usuarioService, NotificacaoService notificacoes,
                           AuditoriaService auditoria, Clock clock) {
        this.propostaRepository = propostaRepository;
        this.projetoRepository = projetoRepository;
        this.contratacaoRepository = contratacaoRepository;
        this.projetoService = projetoService;
        this.usuarioService = usuarioService;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    @Transactional
    public PropostaResponse enviar(Long projetoId, Long prestadorId, PropostaRequest requisicao) {
        Projeto projeto = projetoService.carregarVisivel(projetoId, prestadorId);
        if (propostaRepository.existsByProjetoIdAndPrestadorId(projetoId, prestadorId)) {
            throw new ConflitoException("Você já enviou uma proposta para este projeto.");
        }
        Proposta proposta = projeto.receberProposta(usuarioService.buscarAtivo(prestadorId),
                requisicao.valor(), requisicao.prazoDias(), requisicao.mensagem());
        propostaRepository.save(proposta);
        notificacoes.notificar(projeto.getCliente(), TipoNotificacao.PROPOSTA_RECEBIDA, projeto.getTitulo(),
                "/projects/" + projetoId);
        auditoria.registrar(EventoAuditoria.PROPOSTA_ENVIADA, prestadorId, TipoRecurso.PROPOSTA, proposta.getId());
        return PropostaResponse.de(proposta);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PropostaResponse> listarDoProjeto(Long projetoId, Long clienteId, Pageable pageable) {
        projetoService.carregarDoCliente(projetoId, clienteId);
        return PaginaResponse.de(propostaRepository.findByProjetoId(projetoId, pageable), PropostaResponse::de);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PropostaResponse> listarEnviadas(Long prestadorId, Pageable pageable) {
        return PaginaResponse.de(propostaRepository.findByPrestadorId(prestadorId, pageable), PropostaResponse::de);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PropostaResponse> listarRecebidas(Long clienteId, Pageable pageable) {
        return PaginaResponse.de(propostaRepository.findByProjetoClienteId(clienteId, pageable), PropostaResponse::de);
    }

    /**
     * Aceite = uma unidade transacional: aceita a proposta, cria a contratação com as entregas,
     * muda o projeto para CONTRACTED e recusa as outras propostas. Ou tudo acontece, ou nada.
     * <p>
     * Concorrência: a linha do projeto é travada ({@code SELECT ... FOR UPDATE}) antes de qualquer
     * verificação. Um segundo aceite simultâneo espera, relê o projeto já CONTRACTED e falha na transição.
     * Se algo escapar disso, o UNIQUE em contratacao.projeto_id impede a segunda contratação.
     */
    @Transactional
    public ContratacaoResponse aceitar(Long propostaId, Long clienteId) {
        Proposta proposta = propostaRepository.findById(propostaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta"));
        Projeto projeto = projetoRepository.buscarParaAtualizacao(proposta.getProjeto().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta"));
        if (!proposta.visivelPara(clienteId)) {
            throw new RecursoNaoEncontradoException("Proposta");
        }
        if (!projeto.pertenceA(clienteId)) {
            throw new AcessoNegadoException("Somente o dono do projeto pode aceitar propostas.");
        }
        Instant agora = clock.instant();
        Contratacao contratacao = projeto.aceitarProposta(proposta, agora);
        contratacaoRepository.save(contratacao);
        projetoService.recusarPendentes(projeto, agora);

        notificacoes.notificar(proposta.getPrestador(), TipoNotificacao.PROPOSTA_ACEITA, projeto.getTitulo(),
                "/contracts/" + contratacao.getId());
        auditoria.registrar(EventoAuditoria.PROPOSTA_ACEITA, clienteId, TipoRecurso.PROPOSTA, propostaId);
        auditoria.registrar(EventoAuditoria.CONTRATACAO_CRIADA, clienteId, TipoRecurso.CONTRATACAO, contratacao.getId());
        return ContratacaoResponse.de(contratacao, clienteId, null);
    }

    @Transactional
    public PropostaResponse rejeitar(Long propostaId, Long clienteId) {
        Proposta proposta = carregarVisivel(propostaId, clienteId);
        if (!proposta.getProjeto().pertenceA(clienteId)) {
            throw new AcessoNegadoException("Somente o dono do projeto pode recusar propostas.");
        }
        proposta.rejeitar(clock.instant());
        notificacoes.notificar(proposta.getPrestador(), TipoNotificacao.PROPOSTA_RECUSADA,
                proposta.getProjeto().getTitulo(), "/proposals");
        auditoria.registrar(EventoAuditoria.PROPOSTA_REJEITADA, clienteId, TipoRecurso.PROPOSTA, propostaId);
        return PropostaResponse.de(proposta);
    }

    @Transactional
    public PropostaResponse cancelar(Long propostaId, Long prestadorId) {
        Proposta proposta = carregarVisivel(propostaId, prestadorId);
        if (!proposta.pertenceA(prestadorId)) {
            throw new AcessoNegadoException("Somente quem enviou a proposta pode cancelá-la.");
        }
        proposta.cancelar(clock.instant());
        auditoria.registrar(EventoAuditoria.PROPOSTA_CANCELADA, prestadorId, TipoRecurso.PROPOSTA, propostaId);
        return PropostaResponse.de(proposta);
    }

    private Proposta carregarVisivel(Long propostaId, Long usuarioId) {
        return propostaRepository.findById(propostaId)
                .filter(proposta -> proposta.visivelPara(usuarioId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta"));
    }
}
