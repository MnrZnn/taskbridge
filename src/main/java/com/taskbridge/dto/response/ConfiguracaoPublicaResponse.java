package com.taskbridge.dto.response;

import com.taskbridge.model.enums.ModoPagamento;
import com.taskbridge.model.enums.ProvedorLogin;

import java.util.List;

public record ConfiguracaoPublicaResponse(ModoPagamento modoPagamento, boolean simulacaoPagamentoHabilitada, String versaoTermos,
                                          List<ProvedorLogin> provedoresLogin) {
}
