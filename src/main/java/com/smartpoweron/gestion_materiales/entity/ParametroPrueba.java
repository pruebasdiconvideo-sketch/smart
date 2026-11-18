package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "parametro_prueba")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroPrueba {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idParametro;

    private String tipoPrueba;
    private String unidad;
    private BigDecimal valorMinimo;
    private BigDecimal valorMaximo;
    private String normaReferencia;
    @Builder.Default
    private Boolean activo = true;
}