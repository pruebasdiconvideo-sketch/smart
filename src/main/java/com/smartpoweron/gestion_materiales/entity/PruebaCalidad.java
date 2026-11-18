package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "pruebas_calidad")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PruebaCalidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPrueba;

    private Long idProyecto;
    private Long idParametro;
    private BigDecimal valorMedido;
    private String observacion;
    private String tecnicoExterno;
    private String estado;
    private Boolean anulada;
}