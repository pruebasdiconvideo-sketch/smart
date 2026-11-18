package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "despacho")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Despacho {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDespacho;

    private Long idSolicitud;
    private Long idUsuarioAlmacenero; // <-- Añadido (error DespachoService:198)
    private LocalDateTime fechaDespacho;
    private String tipo;
    @Builder.Default
    private String estado = "PENDIENTE_CONFIRMACION";
    @Builder.Default
    private Boolean anulado = false;
    private String motivoAnulacion;
}