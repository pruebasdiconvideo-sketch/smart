package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Movimiento de stock de solo inserción (RNF07 / P5-RN03).
 * La aplicación NUNCA hace UPDATE ni DELETE sobre esta tabla: el repositorio
 * hereda de JpaRepository pero el servicio sólo invoca save().
 */
@Entity
@Table(name = "trazabilidad")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Trazabilidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_trazabilidad")
    private Long idTrazabilidad;

    @Column(name = "id_material", nullable = false)
    private Long idMaterial;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "id_proyecto")
    private Long idProyecto;

    @Column(name = "id_orden_compra")
    private Long idOrdenCompra;

    @Column(name = "tipo_movimiento", length = 30)
    private String tipoMovimiento;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "fecha_movimiento")
    private LocalDateTime fechaMovimiento;
}
