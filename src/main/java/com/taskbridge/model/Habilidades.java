package com.taskbridge.model;

import com.taskbridge.exception.RegraNegocioException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Normaliza habilidades ("Java ", "java", "JAVA" viram "java") para que o critério de
 * habilidades do matching compare termos equivalentes.
 */
public final class Habilidades {

    public static final int MAXIMO_POR_REGISTRO = 15;
    private static final int TAMANHO_MAXIMO = 40;
    private static final Pattern FORMATO = Pattern.compile("[\\p{L}\\p{N} .#+/-]+");

    private Habilidades() {
    }

    public static Set<String> normalizar(Collection<String> brutas) {
        if (brutas == null) {
            return new LinkedHashSet<>();
        }
        Set<String> normalizadas = new LinkedHashSet<>();
        for (String bruta : brutas) {
            String habilidade = normalizar(bruta);
            if (!habilidade.isEmpty()) {
                normalizadas.add(habilidade);
            }
        }
        if (normalizadas.size() > MAXIMO_POR_REGISTRO) {
            throw new RegraNegocioException("Informe no máximo " + MAXIMO_POR_REGISTRO + " habilidades.");
        }
        return normalizadas;
    }

    private static String normalizar(String bruta) {
        if (bruta == null) {
            return "";
        }
        String habilidade = bruta.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (habilidade.length() > TAMANHO_MAXIMO || (!habilidade.isEmpty() && !FORMATO.matcher(habilidade).matches())) {
            throw new RegraNegocioException("Habilidade inválida: use até " + TAMANHO_MAXIMO + " letras, números ou . # + / -");
        }
        return habilidade;
    }
}
