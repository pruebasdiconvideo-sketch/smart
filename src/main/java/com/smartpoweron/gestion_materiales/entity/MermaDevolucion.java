package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "merma_devolucion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MermaDevolucion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_merma")
    private Long idMerma;

    @Column(name = "id_proyecto", nullable = false)
    private Long idProyecto;

    @Column(name = "id_material", nullable = false)
    private Long idMaterial;

    @Column(name = "id_usuario_registra", nullable = false)
    private Long idUsuarioRegistra;

    @Column(name = "id_usuario_validador")
    private Long idUsuarioValidador;

    @Column(name = "tipo", length = 20)
    private String tipo;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "estado", length = 20)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Column(name = "anulada", nullable = false)
    @Builder.Default
    private Boolean anulada = false;

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;
}
