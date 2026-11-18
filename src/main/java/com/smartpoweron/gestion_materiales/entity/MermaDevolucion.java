package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mermas_devoluciones")
@Data
@Builder // <-- ESTO SOLUCIONA EL ERROR DE .builder()
@NoArgsConstructor
@AllArgsConstructor
public class MermaDevolucion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMerma;

    private Long idProyecto;
    private Long idMaterial;
    private Long idUsuarioRegistra;
    private Long idUsuarioValidador;
    
    private Integer cantidad;
    private String tipo;
    private String estado;
    @Builder.Default
    private Boolean anulada = false;
    private String motivoAnulacion;
    private String motivo;
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();
}