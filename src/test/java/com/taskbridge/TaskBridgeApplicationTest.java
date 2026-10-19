package com.taskbridge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Sobe o contexto completo: migrations do Flyway + validação do schema pelo Hibernate. */
@SpringBootTest
@ActiveProfiles("test")
class TaskBridgeApplicationTest {

    @Test
    void contextoSobeComMigrationsValidadas() {
    }
}
