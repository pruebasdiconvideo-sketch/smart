package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "entrega")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Entrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrega")
    private Long idEntrega;

    @Column(name = "id_despacho", unique = true, nullable = false)
    private Long idDespacho;

    @Column(name = "id_usuario_operario", nullable = false)
    private Long idUsuarioOperario;

    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @Column(name = "confirmado")
    private Boolean confirmado = Boolean.TRUE;
}
