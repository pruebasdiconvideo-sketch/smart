package com.smartpoweron.gestion_materiales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicación principal de Smart Power On S.A.C.
 *
 * Sistema de Gestión de Procesos para el Mejoramiento de la Entrega y Registro
 * de Materiales para el Ensamblaje de Tableros. Informe Cap. 4 - Anexo B.
 *
 * Stack: Spring Boot 3.2.5, Java 21, Thymeleaf, Spring Data JPA, Spring
 * Security con JWT (HS256), MySQL 8 (H2 para pruebas), Bootstrap 5.
 */
@SpringBootApplication
public class GestionMaterialesApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionMaterialesApplication.class, args);
    }
}
