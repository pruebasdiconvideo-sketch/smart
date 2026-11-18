package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prueba_calidad")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PruebaCalidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prueba")
    private Long idPrueba;

    @Column(name = "id_proyecto", nullable = false)
    private Long idProyecto;

    @Column(name = "id_parametro", nullable = false)
    private Long idParametro;

    @Column(name = "id_usuario_inspector", nullable = false)
    private Long idUsuarioInspector;

    @Column(name = "valor_medido", precision = 10, scale = 3)
    private BigDecimal valorMedido;

    @Column(name = "resultado", length = 20)
    private String resultado;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "fecha_prueba")
    private LocalDateTime fechaPrueba;

    @Column(name = "anulada", nullable = false)
    @Builder.Default
    private Boolean anulada = false;

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;

    @Column(name = "tecnico_externo", length = 150)
    private String tecnicoExterno;
}
