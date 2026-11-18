package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "proyectos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proyecto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idProyecto;

    private String codigo;
    private String cliente;
    private Long idEmpresa;
    private LocalDate fechaInicio;
    private Double presupuesto; // O BigDecimal si prefieres, pero revisa el servicio
    private String estado;
}