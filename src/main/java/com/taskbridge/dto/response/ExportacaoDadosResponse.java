package com.taskbridge.dto.response;

import com.taskbridge.model.enums.ProvedorLogin;

import java.time.Instant;
import java.util.List;

/** Direito de acesso/portabilidade (LGPD art. 18): tudo o que a plataforma guarda sobre o titular. */
public record ExportacaoDadosResponse(
        Instant geradoEm,
        UsuarioResponse conta,
        String termosVersao,
        Instant termosAceitosEm,
        PerfilProfissionalResponse perfilProfissional,
        List<ServicoResponse> servicos,
        List<ProjetoResumoResponse> projetos,
        List<PropostaResponse> propostas,
        List<ContratacaoResumoResponse> contratacoes,
        List<RegistroAuditoriaResponse> eventosDeSeguranca,
        List<ProvedorLogin> loginSocial) {
}
