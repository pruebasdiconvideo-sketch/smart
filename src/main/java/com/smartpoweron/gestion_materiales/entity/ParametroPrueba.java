package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "parametro_prueba")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParametroPrueba {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametro")
    private Long idParametro;

    @Column(name = "tipo_prueba", length = 50)
    private String tipoPrueba;

    @Column(name = "unidad", length = 20)
    private String unidad;

    @Column(name = "valor_minimo", precision = 10, scale = 3)
    private BigDecimal valorMinimo;

    @Column(name = "valor_maximo", precision = 10, scale = 3)
    private BigDecimal valorMaximo;

    @Column(name = "norma_referencia", length = 150)
    private String normaReferencia;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
