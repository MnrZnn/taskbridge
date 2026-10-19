package com.taskbridge.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.taskbridge.config.RateLimitProperties.Limite;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

import java.time.Duration;

/**
 * Token bucket (Bucket4j) por chave. O mapa de buckets fica num cache com tamanho máximo:
 * sem esse teto, um atacante trocando de IP a cada requisição faria a memória crescer sem limite.
 * <p>
 * Os contadores ficam na memória desta instância. Com várias instâncias atrás de um balanceador,
 * o próximo passo é o módulo JDBC/Redis do Bucket4j — ou aplicar o limite no proxy/WAF.
 */
public class LimitadorRequisicoes {

    private final Cache<String, Bucket> buckets;

    public LimitadorRequisicoes(long maximoChaves, Duration expiracaoInatividade) {
        this.buckets = Caffeine.newBuilder()
                .maximumSize(maximoChaves)
                .expireAfterAccess(expiracaoInatividade)
                .build();
    }

    public ConsumptionProbe consumir(String chave, Limite limite) {
        Bucket bucket = buckets.get(chave, ignorada -> novoBucket(limite));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    private static Bucket novoBucket(Limite limite) {
        Bandwidth banda = Bandwidth.builder()
                .capacity(limite.capacidade())
                .refillGreedy(limite.capacidade(), limite.periodo())
                .build();
        return Bucket.builder().addLimit(banda).build();
    }
}
