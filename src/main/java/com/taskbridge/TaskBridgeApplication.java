package com.taskbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class TaskBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskBridgeApplication.class, args);
    }

    /** Relógio injetável: regras com prazo (bloqueio de login, expiração do PIX) ficam testáveis. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
