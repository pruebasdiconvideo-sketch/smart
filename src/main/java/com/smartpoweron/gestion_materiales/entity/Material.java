package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "materiales")
@Data
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
    private Integer stockActual = 0;
    private Integer stockMinimo = 0;
    private String estado = "ACTIVO";
    private String pasillo;
    private String estante;
    private String nivel;
}