package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_orden_compra")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleOrdenCompra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDetalleOc;

    private Long idOrdenCompra;
    private Long idMaterial;
    private Integer cantidad;
    private BigDecimal precioUnitario;
}