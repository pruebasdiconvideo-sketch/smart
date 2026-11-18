package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "proyectos")
@Data
public class Proyecto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idProyecto;

    private String codigo;
    private String cliente;
    private Long idEmpresa;
    private LocalDate fechaInicio;
    private Double presupuesto;
    private String estado;
}