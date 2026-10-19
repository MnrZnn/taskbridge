package com.taskbridge.service;

import com.taskbridge.model.enums.ContratacaoStatus;
import com.taskbridge.model.enums.EventoAuditoria;
import com.taskbridge.model.enums.ProjetoStatus;
import com.taskbridge.model.enums.PropostaStatus;
import com.taskbridge.model.enums.TipoRecurso;
import com.taskbridge.model.IdentidadeExterna;
import com.taskbridge.model.Projeto;
import com.taskbridge.model.Proposta;
import com.taskbridge.model.Usuario;
import com.taskbridge.dto.request.ExcluirContaRequest;
import com.taskbridge.dto.response.ContratacaoResumoResponse;
import com.taskbridge.dto.response.ExportacaoDadosResponse;
import com.taskbridge.dto.response.PerfilProfissionalResponse;
import com.taskbridge.dto.response.ProjetoResumoResponse;
import com.taskbridge.dto.response.PropostaResponse;
import com.taskbridge.dto.response.RegistroAuditoriaResponse;
import com.taskbridge.dto.response.ServicoResponse;
import com.taskbridge.dto.response.UsuarioResponse;
import com.taskbridge.exception.ConflitoException;
import com.taskbridge.repository.ContratacaoRepository;
import com.taskbridge.repository.IdentidadeExternaRepository;
import com.taskbridge.repository.NotificacaoRepository;
import com.taskbridge.repository.PerfilProfissionalRepository;
import com.taskbridge.repository.ProjetoRepository;
import com.taskbridge.repository.PropostaRepository;
import com.taskbridge.repository.ServicoRepository;
import com.taskbridge.storage.ArmazenamentoArquivos;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Direitos do titular (LGPD): acesso/portabilidade e eliminação.
 * <p>
 * A exclusão não apaga a linha do usuário. Contratações e pagamentos concluídos continuam existindo
 * porque podem ser exigidos por obrigação legal (fiscal/contábil); o que some são os dados pessoais,
 * que viram valores anônimos. O que só existia por causa da conta (perfil, serviços, rascunhos,
 * notificações, foto) é apagado de fato. Os prazos reais de retenção precisam ser definidos juridicamente.
 */
@Service
public class ContaService {

    private static final Set<ContratacaoStatus> EM_ANDAMENTO = Set.of(ContratacaoStatus.AWAITING_PAYMENT, ContratacaoStatus.IN_PROGRESS);
    private static final Set<ProjetoStatus> ABERTOS = Set.of(ProjetoStatus.PUBLISHED, ProjetoStatus.IN_NEGOTIATION);

    private final UsuarioService usuarioService;
    private final PerfilProfissionalRepository perfilRepository;
    private final ServicoRepository servicoRepository;
    private final ProjetoRepository projetoRepository;
    private final PropostaRepository propostaRepository;
    private final ContratacaoRepository contratacaoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final IdentidadeExternaRepository identidadeRepository;
    private final ProjetoService projetoService;
    private final ArmazenamentoArquivos armazenamento;
    private final AuditoriaService auditoria;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public ContaService(UsuarioService usuarioService, PerfilProfissionalRepository perfilRepository,
                        ServicoRepository servicoRepository, ProjetoRepository projetoRepository,
                        PropostaRepository propostaRepository, ContratacaoRepository contratacaoRepository,
                        NotificacaoRepository notificacaoRepository, IdentidadeExternaRepository identidadeRepository,
                        ProjetoService projetoService,
                        ArmazenamentoArquivos armazenamento, AuditoriaService auditoria,
                        PasswordEncoder passwordEncoder, Clock clock) {
        this.usuarioService = usuarioService;
        this.perfilRepository = perfilRepository;
        this.servicoRepository = servicoRepository;
        this.projetoRepository = projetoRepository;
        this.propostaRepository = propostaRepository;
        this.contratacaoRepository = contratacaoRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.identidadeRepository = identidadeRepository;
        this.projetoService = projetoService;
        this.armazenamento = armazenamento;
        this.auditoria = auditoria;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public ExportacaoDadosResponse exportar(Long usuarioId) {
        Usuario usuario = usuarioService.buscarAtivo(usuarioId);
        Pageable tudo = Pageable.unpaged(Sort.by("id"));
        ExportacaoDadosResponse exportacao = new ExportacaoDadosResponse(
                clock.instant(),
                UsuarioResponse.de(usuario),
                usuario.getTermosVersao(),
                usuario.getTermosAceitosEm(),
                perfilRepository.findByUsuarioId(usuarioId).map(PerfilProfissionalResponse::de).orElse(null),
                servicoRepository.findByPrestadorId(usuarioId, tudo).map(ServicoResponse::de).toList(),
                projetoRepository.findByClienteId(usuarioId, tudo).map(ProjetoResumoResponse::de).toList(),
                propostaRepository.findByPrestadorId(usuarioId, tudo).map(PropostaResponse::de).toList(),
                contratacaoRepository.buscarPorParticipante(usuarioId, tudo).map(ContratacaoResumoResponse::de).toList(),
                auditoria.listarDoUsuario(usuarioId, tudo).map(RegistroAuditoriaResponse::de).toList(),
                identidadeRepository.findByUsuarioId(usuarioId).stream().map(IdentidadeExterna::getProvedor).toList());
        auditoria.registrar(EventoAuditoria.DADOS_EXPORTADOS, usuarioId);
        return exportacao;
    }

    @Transactional
    public void excluir(Long usuarioId, ExcluirContaRequest requisicao) {
        Usuario usuario = usuarioService.buscarAtivo(usuarioId);
        if (usuario.isPossuiSenha()) {
            usuarioService.confirmarSenha(usuario, requisicao.senhaAtual());
        }
        if (contratacaoRepository.existeComParticipanteEStatus(usuarioId, EM_ANDAMENTO)) {
            throw new ConflitoException("Você tem contratações em andamento. Conclua-as antes de excluir a conta.");
        }
        Instant agora = clock.instant();
        encerrarAtividadeComoCliente(usuarioId, agora);
        encerrarAtividadeComoPrestador(usuarioId, agora);
        notificacaoRepository.excluirDoUsuario(usuarioId);
        identidadeRepository.excluirDoUsuario(usuarioId);

        String foto = usuario.getFotoPerfil();
        usuario.anonimizar(passwordEncoder.encode(UUID.randomUUID().toString()));
        if (foto != null) {
            FotoPerfilService.aposTransacao(foto, null, armazenamento);
        }
        auditoria.registrar(EventoAuditoria.CONTA_EXCLUIDA, usuarioId, TipoRecurso.USUARIO, usuarioId);
    }

    private void encerrarAtividadeComoCliente(Long usuarioId, Instant agora) {
        for (Projeto projeto : projetoRepository.findByClienteIdAndStatusIn(usuarioId, Set.of(ProjetoStatus.DRAFT))) {
            projetoRepository.delete(projeto);
        }
        for (Projeto projeto : projetoRepository.findByClienteIdAndStatusIn(usuarioId, ABERTOS)) {
            projeto.cancelar();
            projetoService.recusarPendentes(projeto, agora);
        }
    }

    private void encerrarAtividadeComoPrestador(Long usuarioId, Instant agora) {
        for (Proposta proposta : propostaRepository.findByPrestadorIdAndStatus(usuarioId, PropostaStatus.PENDING)) {
            proposta.cancelar(agora);
        }
        servicoRepository.deleteAll(servicoRepository.findByPrestadorId(usuarioId));
        perfilRepository.findByUsuarioId(usuarioId).ifPresent(perfilRepository::delete);
    }
}
