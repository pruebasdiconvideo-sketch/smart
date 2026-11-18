package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "orden_compra")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOrdenCompra;

    @Column(unique = true)
    private String codigo;
    private String proveedor;
    private Long idEmpresa;
    private LocalDate fechaEmision;
    @Builder.Default
    private String estado = "ABIERTA"; // ABIERTA, CERRADA
}