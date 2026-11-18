package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "material")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMaterial;

    @Column(unique = true, nullable = false)
    private String codigo;
    private String nombre;
    private String descripcion;
    private String categoria;
    private String marca;
    private String modelo;
    private String mlfb;
    private String unidadMedida;
    private String especificacionTecnica;
    private BigDecimal precioUnitario;
    private Integer stockActual;
    private Integer stockMinimo;
    @Builder.Default
    private String estado = "ACTIVO"; // ACTIVO, INACTIVO
    private String pasillo;
    private String estante;
    private String nivel;
}