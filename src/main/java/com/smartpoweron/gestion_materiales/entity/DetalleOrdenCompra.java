package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Línea de una orden de compra. Una OC puede tener uno o varios materiales
 * de varias categorías. Cada línea vincula un material del catálogo con la
 * cantidad solicitada y el precio unitario acordado.
 */
@Entity
@Table(name = "detalle_orden_compra")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetalleOrdenCompra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_oc")
    private Long idDetalleOc;

    @Column(name = "id_orden_compra", nullable = false)
    private Long idOrdenCompra;

    @Column(name = "id_material", nullable = false)
    private Long idMaterial;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", precision = 12, scale = 2)
    private BigDecimal precioUnitario;
}
