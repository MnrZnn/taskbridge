package com.taskbridge.service;

import com.taskbridge.model.enums.ContratacaoStatus;
import com.taskbridge.model.enums.EntregaStatus;
import com.taskbridge.model.enums.ProjetoStatus;
import com.taskbridge.model.enums.PropostaStatus;
import com.taskbridge.model.enums.ServicoStatus;
import com.taskbridge.model.enums.TipoConta;
import com.taskbridge.model.Projeto;
import com.taskbridge.model.Usuario;
import com.taskbridge.dto.response.ContratacaoResumoResponse;
import com.taskbridge.dto.response.DashboardResponse;
import com.taskbridge.dto.response.DashboardResponse.Indicador;
import com.taskbridge.dto.response.DashboardResponse.SugestoesProjeto;
import com.taskbridge.dto.response.ProjetoResumoResponse;
import com.taskbridge.repository.ContratacaoRepository;
import com.taskbridge.repository.EntregaRepository;
import com.taskbridge.repository.ProjetoRepository;
import com.taskbridge.repository.PropostaRepository;
import com.taskbridge.repository.ServicoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** Contadores e listas curtas — o painel mostra o que pede ação, não gráficos. */
@Service
public class DashboardService {

    private static final int ITENS_POR_LISTA = 5;
    private static final int SUGESTOES = 3;
    private static final Set<ProjetoStatus> PROJETOS_ATIVOS = Set.of(ProjetoStatus.DRAFT, ProjetoStatus.PUBLISHED,
            ProjetoStatus.IN_NEGOTIATION, ProjetoStatus.CONTRACTED, ProjetoStatus.IN_PROGRESS);
    private static final Set<ProjetoStatus> PROJETOS_ABERTOS = Set.of(ProjetoStatus.PUBLISHED, ProjetoStatus.IN_NEGOTIATION);
    private static final Set<ContratacaoStatus> CONTRATACOES_ATIVAS = Set.of(ContratacaoStatus.AWAITING_PAYMENT, ContratacaoStatus.IN_PROGRESS);
    private static final Set<EntregaStatus> AGUARDANDO_CLIENTE = Set.of(EntregaStatus.SUBMITTED, EntregaStatus.UNDER_REVIEW);
    private static final Set<EntregaStatus> AGUARDANDO_PRESTADOR = Set.of(EntregaStatus.PENDING, EntregaStatus.REVISION_REQUIRED);

    private final UsuarioService usuarioService;
    private final ProjetoRepository projetoRepository;
    private final PropostaRepository propostaRepository;
    private final ContratacaoRepository contratacaoRepository;
    private final EntregaRepository entregaRepository;
    private final ServicoRepository servicoRepository;
    private final MatchingService matchingService;

    public DashboardService(UsuarioService usuarioService, ProjetoRepository projetoRepository,
                            PropostaRepository propostaRepository, ContratacaoRepository contratacaoRepository,
                            EntregaRepository entregaRepository, ServicoRepository servicoRepository,
                            MatchingService matchingService) {
        this.usuarioService = usuarioService;
        this.projetoRepository = projetoRepository;
        this.propostaRepository = propostaRepository;
        this.contratacaoRepository = contratacaoRepository;
        this.entregaRepository = entregaRepository;
        this.servicoRepository = servicoRepository;
        this.matchingService = matchingService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse montar(Long usuarioId) {
        Usuario usuario = usuarioService.buscarAtivo(usuarioId);
        return usuario.isCliente() ? painelDoCliente(usuarioId) : painelDoPrestador(usuarioId);
    }

    private DashboardResponse painelDoCliente(Long clienteId) {
        List<Projeto> ativos = projetoRepository.findByClienteIdAndStatusInOrderByCriadoEmDesc(
                clienteId, PROJETOS_ATIVOS, PageRequest.of(0, ITENS_POR_LISTA));
        List<Indicador> indicadores = List.of(
                new Indicador("projetosAtivos", "Projetos ativos",
                        projetoRepository.countByClienteIdAndStatusIn(clienteId, PROJETOS_ATIVOS), "/projects?aba=meus"),
                new Indicador("propostasPendentes", "Propostas recebidas",
                        propostaRepository.countByProjetoClienteIdAndStatus(clienteId, PropostaStatus.PENDING), "/proposals"),
                new Indicador("contratacoesAtivas", "Contratações ativas",
                        contratacaoRepository.countByProjetoClienteIdAndStatusIn(clienteId, CONTRATACOES_ATIVAS), "/contracts"),
                new Indicador("entregasParaRevisar", "Entregas para revisar",
                        entregaRepository.countByContratacaoProjetoClienteIdAndStatusIn(clienteId, AGUARDANDO_CLIENTE), "/contracts"));
        SugestoesProjeto sugestoes = ativos.stream()
                .filter(projeto -> PROJETOS_ABERTOS.contains(projeto.getStatus()))
                .findFirst()
                .map(projeto -> new SugestoesProjeto(projeto.getId(), projeto.getTitulo(),
                        matchingService.profissionaisPara(projeto, SUGESTOES)))
                .orElse(null);
        return new DashboardResponse(TipoConta.CLIENTE, indicadores,
                ativos.stream().map(ProjetoResumoResponse::de).toList(),
                contratacoesAtivas(clienteId), List.of(), sugestoes);
    }

    private DashboardResponse painelDoPrestador(Long prestadorId) {
        List<Indicador> indicadores = List.of(
                new Indicador("servicosAtivos", "Serviços ativos",
                        servicoRepository.countByPrestadorIdAndStatus(prestadorId, ServicoStatus.ATIVO), "/services?aba=meus"),
                new Indicador("propostasEnviadas", "Propostas aguardando resposta",
                        propostaRepository.countByPrestadorIdAndStatus(prestadorId, PropostaStatus.PENDING), "/proposals"),
                new Indicador("contratacoesAtivas", "Contratações ativas",
                        contratacaoRepository.countByPropostaPrestadorIdAndStatusIn(prestadorId, CONTRATACOES_ATIVAS), "/contracts"),
                new Indicador("entregasPendentes", "Entregas a enviar",
                        entregaRepository.countByContratacaoPropostaPrestadorIdAndContratacaoStatusAndStatusIn(
                                prestadorId, ContratacaoStatus.IN_PROGRESS, AGUARDANDO_PRESTADOR), "/contracts"));
        return new DashboardResponse(TipoConta.PRESTADOR, indicadores, List.of(),
                contratacoesAtivas(prestadorId),
                matchingService.projetosCompativeis(prestadorId, ITENS_POR_LISTA), null);
    }

    private List<ContratacaoResumoResponse> contratacoesAtivas(Long usuarioId) {
        return contratacaoRepository.buscarPorParticipanteEStatus(usuarioId, CONTRATACOES_ATIVAS,
                        PageRequest.of(0, ITENS_POR_LISTA)).stream()
                .map(ContratacaoResumoResponse::de)
                .toList();
    }
}
