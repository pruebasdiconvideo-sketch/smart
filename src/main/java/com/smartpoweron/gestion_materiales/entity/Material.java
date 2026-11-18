package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "material")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_material")
    private Long idMaterial;

    @Column(name = "codigo", unique = true, nullable = false, length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "especificacion_tecnica", columnDefinition = "TEXT")
    private String especificacionTecnica;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "categoria", length = 50)
    private String categoria;

    @Column(length = 50)
    private String marca;

    @Column(name = "unidad_medida", length = 20)
    private String unidadMedida;

    @Column(length = 100)
    private String modelo;

    @Column(name = "numero_serie", length = 100)
    private String numeroSerie;

    @Column(length = 100)
    private String mlfb;

    @Column(length = 20)
    private String pasillo;

    @Column(length = 20)
    private String estante;

    @Column(length = 20)
    private String nivel;

    @Column(length = 20)
    private String estado = "ACTIVO";

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual = 0;

    @Column(name = "stock_minimo")
    private Integer stockMinimo = 0;
}
