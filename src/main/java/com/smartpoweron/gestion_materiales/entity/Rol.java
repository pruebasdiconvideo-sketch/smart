package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rol")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRol;
    private String nombre;
    private String descripcion;
}