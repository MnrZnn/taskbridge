package com.taskbridge.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Formato único de erro da API. Nunca inclui stack trace, SQL ou nome de classe interna. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErroResponse(
        int status,
        String message,
        Instant timestamp,
        String path,
        List<String> detalhes,
        Map<String, String> campos) {

    public static ErroResponse de(int status, String message, String path) {
        return new ErroResponse(status, message, Instant.now(), path, List.of(), Map.of());
    }

    public static ErroResponse comDetalhes(int status, String message, String path, List<String> detalhes) {
        return new ErroResponse(status, message, Instant.now(), path, detalhes, Map.of());
    }

    public static ErroResponse comCampos(int status, String message, String path, Map<String, String> campos) {
        return new ErroResponse(status, message, Instant.now(), path, List.of(), campos);
    }
}
