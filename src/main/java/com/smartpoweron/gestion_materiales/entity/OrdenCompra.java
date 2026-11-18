package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "ordenes_compra")
@Data
public class OrdenCompra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOrdenCompra;

    @Column(unique = true)
    private String codigo;

    private String proveedor;
    private Long idEmpresa;
    private LocalDate fechaEmision;
    private String estado;
}