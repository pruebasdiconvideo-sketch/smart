package com.smartpoweron.gestion_materiales.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class IngresoMaterialDTO {
    @NotNull(message = "Debe seleccionar una orden de compra")
    private Long ordenCompraId;

    @NotBlank(message = "El código del material es obligatorio")
    private String codigoMaterial;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;
}