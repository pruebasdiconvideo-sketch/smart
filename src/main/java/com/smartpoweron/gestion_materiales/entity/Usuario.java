package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Data
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Column(unique = true)
    private String username;

    private String passwordHash;
    private String nombreCompleto;
    private String correo;
    private Long idRol;
    private Boolean activo;
    private LocalDateTime ultimoAcceso;
}