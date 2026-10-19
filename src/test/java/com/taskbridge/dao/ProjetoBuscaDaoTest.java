package com.taskbridge.dao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjetoBuscaDaoTest {

    @Test
    void curingasDoLikeDigitadosPeloUsuarioViramTextoLiteral() {
        assertThat(ProjetoBuscaDao.escaparLike("100%_off\\")).isEqualTo("100\\%\\_off\\\\");
    }
}
