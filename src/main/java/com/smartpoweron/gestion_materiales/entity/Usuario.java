package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    private String passwordHash;
    private String nombreCompleto;
    private String correo;
    private Long idRol;
    @Builder.Default
    private Boolean activo = true;
    private LocalDateTime ultimoAcceso;
}