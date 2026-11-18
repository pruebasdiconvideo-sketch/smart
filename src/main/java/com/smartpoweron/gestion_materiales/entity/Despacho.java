package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "despacho")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Despacho {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_despacho")
    private Long idDespacho;

    @Column(name = "id_solicitud", nullable = false)
    private Long idSolicitud;

    @Column(name = "id_usuario_almacenero", nullable = false)
    private Long idUsuarioAlmacenero;

    @Column(name = "fecha_despacho")
    private LocalDateTime fechaDespacho;

    @Column(name = "tipo", length = 20)
    private String tipo;

    @Column(name = "estado", length = 30)
    private String estado = "PENDIENTE_CONFIRMACION";

    @Column(name = "anulado", nullable = false)
    @Builder.Default
    private Boolean anulado = false;

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;
}
