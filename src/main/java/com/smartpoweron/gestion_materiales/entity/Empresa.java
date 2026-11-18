package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "empresas")
@Data
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEmpresa;

    private String nombre;
    private String ruc;
    private String direccion;
    private String telefono;
}