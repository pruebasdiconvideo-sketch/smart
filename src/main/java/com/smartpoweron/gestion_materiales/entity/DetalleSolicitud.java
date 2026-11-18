package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "detalle_solicitud")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleSolicitud {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDetalleSolicitud;

    private Long idSolicitud;
    private Long idMaterial;
    private Integer cantidadSolicitada;
    private Integer cantidadDespachada;   // ← necesario para el servicio
}