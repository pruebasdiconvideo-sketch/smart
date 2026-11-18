package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "detalle_despacho")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetalleDespacho {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_despacho")
    private Long idDetalleDespacho;

    @Column(name = "id_despacho", nullable = false)
    private Long idDespacho;

    @Column(name = "id_detalle_solicitud", nullable = false)
    private Long idDetalleSolicitud;

    @Column(name = "id_material", nullable = false)
    private Long idMaterial;

    @Column(name = "codigo_digitado", length = 50)
    private String codigoDigitado;

    @Column(name = "cantidad_entregada", nullable = false)
    private Integer cantidadEntregada;
}
