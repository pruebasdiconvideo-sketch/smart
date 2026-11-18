package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "parametros_prueba")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroPrueba {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idParametro;

    private String nombre;
    private String tipoPrueba;
    private String unidad;
    private BigDecimal valorMinimo;
    private BigDecimal valorMaximo;
    private String normaReferencia;
    private Boolean activo;
}