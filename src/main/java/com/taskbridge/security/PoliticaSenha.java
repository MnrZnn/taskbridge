package com.taskbridge.security;

import com.taskbridge.exception.RegraNegocioException;

import java.nio.charset.StandardCharsets;

public final class PoliticaSenha {

    public static final int TAMANHO_MINIMO = 10;
    /** BCrypt ignora o que passa de 72 bytes; aceitar mais daria falsa sensação de segurança. */
    public static final int MAXIMO_BYTES_BCRYPT = 72;

    private PoliticaSenha() {
    }

    public static void validar(String senha) {
        if (senha == null || senha.length() < TAMANHO_MINIMO) {
            throw new RegraNegocioException("A senha deve ter pelo menos " + TAMANHO_MINIMO + " caracteres.");
        }
        if (excedeLimiteBcrypt(senha)) {
            throw new RegraNegocioException("A senha é longa demais.");
        }
        if (senha.chars().noneMatch(Character::isLetter) || senha.chars().noneMatch(Character::isDigit)) {
            throw new RegraNegocioException("A senha deve conter letras e números.");
        }
    }

    public static boolean excedeLimiteBcrypt(String senha) {
        return senha.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES_BCRYPT;
    }
}
