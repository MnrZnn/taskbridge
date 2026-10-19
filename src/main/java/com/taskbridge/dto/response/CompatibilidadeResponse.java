package com.taskbridge.dto.response;

import com.taskbridge.model.matching.ResultadoMatching;

import java.util.List;

/** Profissional sugerido para um projeto, com o porquê de cada critério. */
public record CompatibilidadeResponse(
        UsuarioResumoResponse profissional,
        String tituloProfissional,
        int experienciaAnos,
        int pontuacao,
        List<CriterioResponse> criterios) {

    public record CriterioResponse(String criterio, boolean atendido, int pontos, int peso, String explicacao) {

        public static List<CriterioResponse> de(ResultadoMatching resultado) {
            return resultado.criterios().stream()
                    .map(avaliado -> new CriterioResponse(
                            avaliado.avaliacao().criterio(),
                            avaliado.avaliacao().atendido(),
                            (int) Math.round(avaliado.pontos()),
                            avaliado.peso(),
                            avaliado.avaliacao().explicacao()))
                    .toList();
        }
    }
}
