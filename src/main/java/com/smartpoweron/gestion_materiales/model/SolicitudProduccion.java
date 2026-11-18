package com.smartpoweron.gestion_materiales.model;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SolicitudProduccion {
    private Long id;
    private String codigo;
    private String descripcion;
    private LocalDate fechaSolicitud;
    private String estado; // PENDIENTE, APROBADA, RECHAZADA
}