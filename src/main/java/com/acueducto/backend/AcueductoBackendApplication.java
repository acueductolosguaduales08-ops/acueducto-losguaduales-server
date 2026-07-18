package com.acueducto.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sistema Integral de Gestion del Acueducto Los Guaduales.
 * API REST construida con Spring Boot que centraliza la gestion de asociados,
 * medidores, facturacion, tesoreria, encuestas, notificaciones y el portal publico.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class AcueductoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcueductoBackendApplication.class, args);
    }
}
