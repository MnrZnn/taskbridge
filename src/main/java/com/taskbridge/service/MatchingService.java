package com.taskbridge.service;

import com.taskbridge.config.MatchingProperties;
import com.taskbridge.model.enums.ProjetoStatus;
import com.taskbridge.model.enums.ServicoStatus;
import com.taskbridge.model.matching.CalculadoraCompatibilidade;
import com.taskbridge.model.matching.CandidatoProfissional;
import com.taskbridge.model.matching.ResultadoMatching;
import com.taskbridge.model.PerfilProfissional;
import com.taskbridge.model.Projeto;
import com.taskbridge.model.Servico;
import com.taskbridge.dto.response.CompatibilidadeResponse;
import com.taskbridge.dto.response.CompatibilidadeResponse.CriterioResponse;
import com.taskbridge.dto.response.ProjetoCompativelResponse;
import com.taskbridge.dto.response.ProjetoResumoResponse;
import com.taskbridge.dto.response.UsuarioResumoResponse;
import com.taskbridge.repository.PerfilProfissionalRepository;
import com.taskbridge.repository.ProjetoRepository;
import com.taskbridge.repository.PropostaRepository;
import com.taskbridge.repository.ServicoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Carrega candidatos do banco (com limite, para o custo ser previsível) e delega a pontuação
 * à {@link CalculadoraCompatibilidade}, que não sabe nada de banco nem de HTTP.
 */
@Service
public class MatchingService {

    private static final Set<ProjetoStatus> ABERTOS = Set.of(ProjetoStatus.PUBLISHED, ProjetoStatus.IN_NEGOTIATION);
    /** Nenhuma habilidade salva é vazia; usar "" evita um IN () vazio quando o projeto não pede habilidades. */
    private static final Set<String> SEM_HABILIDADES = Set.of("");

    private final CalculadoraCompatibilidade calculadora;
    private final PerfilProfissionalRepository perfilRepository;
    private final ServicoRepository servicoRepository;
    private final ProjetoRepository projetoRepository;
    private final PropostaRepository propostaRepository;
    private final ProjetoService projetoService;
    private final MatchingProperties propriedades;

    public MatchingService(CalculadoraCompatibilidade calculadora, PerfilProfissionalRepository perfilRepository,
                           ServicoRepository servicoRepository, ProjetoRepository projetoRepository,
                           PropostaRepository propostaRepository, ProjetoService projetoService,
                           MatchingProperties propriedades) {
        this.calculadora = calculadora;
        this.perfilRepository = perfilRepository;
        this.servicoRepository = servicoRepository;
        this.projetoRepository = projetoRepository;
        this.propostaRepository = propostaRepository;
        this.projetoService = projetoService;
        this.propriedades = propriedades;
    }

    /** Só o dono do projeto vê quem é compatível com ele. */
    @Transactional(readOnly = true)
    public List<CompatibilidadeResponse> profissionaisCompativeis(Long projetoId, Long clienteId, int limite) {
        return profissionaisPara(projetoService.carregarDoCliente(projetoId, clienteId), limite);
    }

    @Transactional(readOnly = true)
    public List<CompatibilidadeResponse> profissionaisPara(Projeto projeto, int limite) {
        Set<String> habilidades = projeto.getHabilidades().isEmpty() ? SEM_HABILIDADES : projeto.getHabilidades();
        Long categoriaId = projeto.getCategoria() == null ? null : projeto.getCategoria().getId();
        List<PerfilProfissional> perfis = perfilRepository.buscarCandidatos(projeto.getCliente().getId(), categoriaId,
                habilidades, PageRequest.of(0, propriedades.limiteCandidatos()));
        Map<Long, List<Servico>> servicosPorPrestador = servicosAtivosDe(perfis);

        return perfis.stream()
                .map(perfil -> {
                    List<Servico> servicos = servicosPorPrestador.getOrDefault(perfil.getUsuario().getId(), List.of());
                    ResultadoMatching resultado = calculadora.calcular(projeto, new CandidatoProfissional(perfil, servicos));
                    return new CompatibilidadeResponse(UsuarioResumoResponse.de(perfil.getUsuario()),
                            perfil.getTituloProfissional(), perfil.getExperienciaAnos(),
                            resultado.pontuacao(), CriterioResponse.de(resultado));
                })
                .sorted(Comparator.comparingInt(CompatibilidadeResponse::pontuacao).reversed())
                .limit(limite)
                .toList();
    }

    /** Projetos abertos avaliados contra o perfil do prestador. Sem perfil profissional, não há o que comparar. */
    @Transactional(readOnly = true)
    public List<ProjetoCompativelResponse> projetosCompativeis(Long prestadorId, int limite) {
        return perfilRepository.findByUsuarioId(prestadorId)
                .filter(PerfilProfissional::disponivelParaMatching)
                .map(perfil -> avaliarProjetosAbertos(perfil, limite))
                .orElse(List.of());
    }

    private List<ProjetoCompativelResponse> avaliarProjetosAbertos(PerfilProfissional perfil, int limite) {
        Long prestadorId = perfil.getUsuario().getId();
        CandidatoProfissional candidato = new CandidatoProfissional(perfil,
                servicoRepository.findByPrestadorIdAndStatusOrderByCriadoEmDesc(prestadorId, ServicoStatus.ATIVO));
        Set<Long> comProposta = new HashSet<>(propostaRepository.idsDeProjetosComPropostaDe(prestadorId));
        List<Projeto> abertos = projetoRepository.findByStatusInOrderByPublicadoEmDesc(ABERTOS,
                PageRequest.of(0, propriedades.limiteCandidatos()));

        return abertos.stream()
                .map(projeto -> {
                    ResultadoMatching resultado = calculadora.calcular(projeto, candidato);
                    return new ProjetoCompativelResponse(ProjetoResumoResponse.de(projeto), resultado.pontuacao(),
                            CriterioResponse.de(resultado), comProposta.contains(projeto.getId()));
                })
                .sorted(Comparator.comparingInt(ProjetoCompativelResponse::pontuacao).reversed())
                .limit(limite)
                .toList();
    }

    private Map<Long, List<Servico>> servicosAtivosDe(List<PerfilProfissional> perfis) {
        if (perfis.isEmpty()) {
            return Map.of();
        }
        List<Long> prestadores = perfis.stream().map(perfil -> perfil.getUsuario().getId()).toList();
        return servicoRepository.findByPrestadorIdInAndStatus(prestadores, ServicoStatus.ATIVO).stream()
                .collect(Collectors.groupingBy(servico -> servico.getPrestador().getId()));
    }
}
