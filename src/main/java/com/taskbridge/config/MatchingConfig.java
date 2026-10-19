package com.taskbridge.config;

import com.taskbridge.model.matching.CalculadoraCompatibilidade;
import com.taskbridge.model.matching.CalculadoraCompatibilidade.CriterioPonderado;
import com.taskbridge.model.matching.CompatibilidadeCategoria;
import com.taskbridge.model.matching.CompatibilidadeDisponibilidade;
import com.taskbridge.model.matching.CompatibilidadeHabilidades;
import com.taskbridge.model.matching.CompatibilidadeOrcamento;
import com.taskbridge.model.matching.CompatibilidadePrazo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Monta as estratégias de matching com os pesos vindos de configuração. */
@Configuration
public class MatchingConfig {

    @Bean
    CalculadoraCompatibilidade calculadoraCompatibilidade(MatchingProperties pesos) {
        return new CalculadoraCompatibilidade(List.of(
                new CriterioPonderado(new CompatibilidadeCategoria(), pesos.pesoCategoria()),
                new CriterioPonderado(new CompatibilidadeHabilidades(), pesos.pesoHabilidades()),
                new CriterioPonderado(new CompatibilidadeOrcamento(), pesos.pesoOrcamento()),
                new CriterioPonderado(new CompatibilidadePrazo(), pesos.pesoPrazo()),
                new CriterioPonderado(new CompatibilidadeDisponibilidade(), pesos.pesoDisponibilidade())));
    }
}
