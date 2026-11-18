package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "despachos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Despacho {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDespacho;

    private Long idSolicitud;
    private LocalDateTime fechaDespacho;
    private String tipo;
    private String estado;
    private Boolean anulado;
    private String motivoAnulacion;   // ← necesario para el servicio
}