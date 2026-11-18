package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "detalle_solicitud")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetalleSolicitud {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_solicitud")
    private Long idDetalleSolicitud;

    @Column(name = "id_solicitud", nullable = false)
    private Long idSolicitud;

    @Column(name = "id_material", nullable = false)
    private Long idMaterial;

    @Column(name = "cantidad_solicitada", nullable = false)
    private Integer cantidadSolicitada;

    @Column(name = "cantidad_despachada")
    private Integer cantidadDespachada = 0;
}
