package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prueba_calidad")
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
    private Long idUsuarioInspector; // <-- Añadido (error CalidadService:81)
    private BigDecimal valorMedido;
    private String observacion;
    private String tecnicoExterno;
    @Builder.Default
    private Boolean anulada = false;
    private String motivoAnulacion;  // <-- Añadido (error CalidadService:100,113)
    private String resultado;        // <-- Añadido (error CalidadService:216,239)
    private LocalDateTime fechaPrueba; // <-- Añadido (error CalidadService:243)
}