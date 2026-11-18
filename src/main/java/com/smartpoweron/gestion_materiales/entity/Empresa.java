package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "empresas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEmpresa;

    private String ruc;
    private String razonSocial;
    private String razonComercial;
    private String tipo; // CLIENTE, PROVEEDOR, AMBOS
    private String contactoNombre;
    private String contactoTelefono;
    private String contactoCorreo;
    private String estado; // ACTIVO, INACTIVO
    private LocalDate fechaRegistro;
}