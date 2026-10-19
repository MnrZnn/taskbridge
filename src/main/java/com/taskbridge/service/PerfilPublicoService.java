package com.taskbridge.service;

import com.taskbridge.model.enums.ContratacaoStatus;
import com.taskbridge.model.enums.ServicoStatus;
import com.taskbridge.model.enums.UsuarioStatus;
import com.taskbridge.model.Contratacao;
import com.taskbridge.model.Usuario;
import com.taskbridge.dto.response.PerfilProfissionalResponse;
import com.taskbridge.dto.response.PerfilPublicoResponse;
import com.taskbridge.dto.response.PerfilPublicoResponse.Estatisticas;
import com.taskbridge.dto.response.PerfilPublicoResponse.ItemHistorico;
import com.taskbridge.dto.response.ServicoResponse;
import com.taskbridge.dto.response.UsuarioResumoResponse;
import com.taskbridge.exception.RecursoNaoEncontradoException;
import com.taskbridge.repository.ContratacaoRepository;
import com.taskbridge.repository.PerfilProfissionalRepository;
import com.taskbridge.repository.ServicoRepository;
import com.taskbridge.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Perfil acessado por nome de usuário, não por id: não dá para percorrer /usuarios/1, /2, /3...
 * Contas excluídas (anonimizadas) respondem 404.
 */
@Service
public class PerfilPublicoService {

    private static final int ITENS_HISTORICO = 10;
    private static final Set<ContratacaoStatus> ATIVAS = Set.of(ContratacaoStatus.AWAITING_PAYMENT, ContratacaoStatus.IN_PROGRESS);

    private final UsuarioRepository usuarioRepository;
    private final PerfilProfissionalRepository perfilRepository;
    private final ServicoRepository servicoRepository;
    private final ContratacaoRepository contratacaoRepository;

    public PerfilPublicoService(UsuarioRepository usuarioRepository, PerfilProfissionalRepository perfilRepository,
                                ServicoRepository servicoRepository, ContratacaoRepository contratacaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.servicoRepository = servicoRepository;
        this.contratacaoRepository = contratacaoRepository;
    }

    @Transactional(readOnly = true)
    public PerfilPublicoResponse buscar(String nomeUsuario) {
        Usuario usuario = usuarioRepository
                .findByNomeUsuarioAndStatus(Usuario.normalizarNomeUsuario(nomeUsuario), UsuarioStatus.ATIVO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário"));
        return usuario.isPrestador() ? perfilDePrestador(usuario) : perfilDeCliente(usuario);
    }

    private PerfilPublicoResponse perfilDePrestador(Usuario usuario) {
        Long id = usuario.getId();
        List<ServicoResponse> servicos = servicoRepository
                .findByPrestadorIdAndStatusOrderByCriadoEmDesc(id, ServicoStatus.ATIVO).stream()
                .map(ServicoResponse::de).toList();
        PerfilProfissionalResponse perfil = perfilRepository.findByUsuarioId(id).map(PerfilProfissionalResponse::de).orElse(null);
        Estatisticas estatisticas = new Estatisticas(
                contratacaoRepository.countByPropostaPrestadorIdAndStatus(id, ContratacaoStatus.COMPLETED),
                servicos.size(),
                contratacaoRepository.countByPropostaPrestadorIdAndStatusIn(id, ATIVAS));
        List<Contratacao> concluidas = contratacaoRepository.findByPropostaPrestadorIdAndStatusOrderByConcluidaEmDesc(
                id, ContratacaoStatus.COMPLETED, PageRequest.of(0, ITENS_HISTORICO));
        return montar(usuario, perfil, servicos, estatisticas, concluidas);
    }

    private PerfilPublicoResponse perfilDeCliente(Usuario usuario) {
        Long id = usuario.getId();
        Estatisticas estatisticas = new Estatisticas(
                contratacaoRepository.countByProjetoClienteIdAndStatus(id, ContratacaoStatus.COMPLETED),
                0,
                contratacaoRepository.countByProjetoClienteIdAndStatusIn(id, ATIVAS));
        List<Contratacao> concluidas = contratacaoRepository.findByProjetoClienteIdAndStatusOrderByConcluidaEmDesc(
                id, ContratacaoStatus.COMPLETED, PageRequest.of(0, ITENS_HISTORICO));
        return montar(usuario, null, List.of(), estatisticas, concluidas);
    }

    private static PerfilPublicoResponse montar(Usuario usuario, PerfilProfissionalResponse perfil,
                                                List<ServicoResponse> servicos, Estatisticas estatisticas,
                                                List<Contratacao> concluidas) {
        List<ItemHistorico> historico = concluidas.stream()
                .map(contratacao -> new ItemHistorico(
                        contratacao.getProjeto().getTitulo(),
                        contratacao.getProjeto().getCategoria() == null ? null : contratacao.getProjeto().getCategoria().getNome(),
                        contratacao.getConcluidaEm()))
                .toList();
        UsuarioResumoResponse resumo = UsuarioResumoResponse.de(usuario);
        return new PerfilPublicoResponse(
                resumo.nomeUsuario(),
                resumo.nomeCompleto(),
                usuario.getTipoConta(),
                usuario.getBiografia(),
                usuario.getLocalizacao(),
                resumo.fotoUrl(),
                usuario.getLinks(),
                usuario.getDataCadastro(),
                perfil,
                servicos,
                estatisticas,
                historico);
    }
}
