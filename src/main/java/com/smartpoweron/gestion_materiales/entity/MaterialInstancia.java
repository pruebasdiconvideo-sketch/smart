package com.smartpoweron.gestion_materiales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Instancia física de un material (una unidad) identificada por su número de serie único.
 *
 * Se crea al registrar un ingreso contra una orden de compra: por cada unidad que
 * ingresa al almacén se registra una fila con su número de serie, el modelo de
 * material (id_material), la OC de origen y la fecha. El stock consolidado por
 * modelo/código/marca vive en {@link Material#getStockActual()}; esta tabla aporta
 * la trazabilidad individual de cada unidad (informe: "cada unidad deberá mantener
 * su propio número de serie para garantizar su identificación y trazabilidad").
 */
@Entity
@Table(name = "material_instancia",
        uniqueConstraints = @UniqueConstraint(name = "uk_instancia_numero_serie", columnNames = "numero_serie"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MaterialInstancia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_instancia")
    private Long idInstancia;

    @Column(name = "id_material", nullable = false)
    private Long idMaterial;

    @Column(name = "numero_serie", nullable = false, length = 100)
    private String numeroSerie;

    @Column(name = "id_orden_compra")
    private Long idOrdenCompra;

    @Column(name = "fecha_ingreso")
    private LocalDateTime fechaIngreso;

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(length = 20)
    private String estado = "EN_ALMACEN"; // EN_ALMACEN | SALIDA | MERMADO
}
