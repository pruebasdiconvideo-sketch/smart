package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "proyecto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Proyecto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proyecto")
    private Long idProyecto;

    @Column(name = "codigo", unique = true, nullable = false, length = 50)
    private String codigo;

    @Column(name = "cliente", nullable = false, length = 150)
    private String cliente;

    @Column(name = "id_empresa")
    private Long idEmpresa;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "presupuesto", nullable = false, precision = 12, scale = 2)
    private BigDecimal presupuesto;

    @Column(name = "estado", length = 30)
    private String estado = "EN_PROCESO";
}
