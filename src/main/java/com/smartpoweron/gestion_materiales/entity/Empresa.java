package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Empresa registrada como proveedor o cliente.
 * Una sola tabla con discriminador {@code tipo} (PROVEEDOR / CLIENTE) para que
 * el usuario no vuelva a digitar los datos de la empresa en cada orden de compra
 * o proyecto. Datos: RUC, razón social, razón comercial y contacto.
 */
@Entity
@Table(name = "empresa")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empresa")
    private Long idEmpresa;

    @Column(name = "ruc", unique = true, nullable = false, length = 11)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 180)
    private String razonSocial;

    @Column(name = "razon_comercial", length = 180)
    private String razonComercial;

    @Column(name = "contacto_nombre", length = 150)
    private String contactoNombre;

    @Column(name = "contacto_telefono", length = 30)
    private String contactoTelefono;

    @Column(name = "contacto_correo", length = 120)
    private String contactoCorreo;

    @Column(name = "tipo", nullable = false, length = 15)
    private String tipo;

    @Column(name = "estado", length = 20)
    @Builder.Default
    private String estado = "ACTIVO";

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;
}
