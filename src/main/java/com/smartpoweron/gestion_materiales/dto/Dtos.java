package com.smartpoweron.gestion_materiales.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    // ===== A0 · Acceso =====
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido")
        private String correo;
        @NotBlank(message = "La contraseña es obligatoria")
        private String password;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoginResponse {
        private String token;
        private String nombreCompleto;
        private String rol;
        private Long idUsuario;
    }

    // ===== Usuarios =====
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UsuarioDto {
        private Long idUsuario;
        private Long idRol;
        private String rol;
        private String nombreCompleto;
        private String correo;
        private Boolean activo;
        private LocalDateTime ultimoAcceso;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UsuarioCreate {
        @NotNull(message = "El rol es obligatorio") private Long idRol;
        @NotBlank(message = "El nombre es obligatorio") private String nombreCompleto;
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") private String correo;
        @NotBlank(message = "La contraseña es obligatoria") private String password;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UsuarioUpdate {
        @NotNull(message = "El rol es obligatorio") private Long idRol;
        @NotBlank(message = "El nombre es obligatorio") private String nombreCompleto;
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") private String correo;
        private String password;
    }

    // ===== M1 · Inventario =====
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MaterialDto {
        private Long idMaterial;
        private String codigo;
        private String nombre;
        private String especificacionTecnica;
        private String descripcion;
        private String categoria;
        private String marca;
        private String unidadMedida;
        private String modelo;
        private String mlfb;
        private String pasillo;
        private String estante;
        private String nivel;
        private String estado;
        private BigDecimal precioUnitario;
        private Integer stockActual;
        private Integer stockMinimo;
        private boolean enAlerta;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoriaStockDto {
        private String categoria;
        private int numMateriales;
        private int stockTotal;
        private int stockMinimoTotal;
        private BigDecimal valorInventario;
        private long enAlerta;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MaterialCreate {
        private String codigo;
        @NotBlank(message = "El nombre es obligatorio") private String nombre;
        private String especificacionTecnica;
        private String descripcion;
        private String categoria;
        private String marca;
        private String unidadMedida;
        private String modelo;
        private String mlfb;
        private String pasillo;
        private String estante;
        private String nivel;
        @NotNull(message = "El precio es obligatorio") @DecimalMin(value = "0.0", message = "El precio no puede ser negativo") private BigDecimal precioUnitario;
        @Min(value = 0, message = "El stock mínimo no puede ser negativo") private Integer stockMinimo;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MaterialUpdate {
        private String codigo;
        @NotBlank(message = "El nombre es obligatorio") private String nombre;
        private String especificacionTecnica;
        private String descripcion;
        private String categoria;
        private String marca;
        private String unidadMedida;
        private String modelo;
        private String mlfb;
        private String pasillo;
        private String estante;
        private String nivel;
        @NotNull(message = "El precio es obligatorio") @DecimalMin(value = "0.0", message = "El precio no puede ser negativo") private BigDecimal precioUnitario;
        @Min(value = 0, message = "El stock mínimo no puede ser negativo") private Integer stockMinimo;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EmpresaDto {
        private Long idEmpresa;
        private String ruc;
        private String razonSocial;
        private String razonComercial;
        private String contactoNombre;
        private String contactoTelefono;
        private String contactoCorreo;
        private String tipo;
        private String estado;
        private LocalDateTime fechaRegistro;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EmpresaCreate {
        @NotBlank(message = "El RUC es obligatorio") private String ruc;
        @NotBlank(message = "La razón social es obligatoria") private String razonSocial;
        private String razonComercial;
        private String contactoNombre;
        private String contactoTelefono;
        private String contactoCorreo;
        @NotBlank(message = "El tipo es obligatorio") private String tipo;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EmpresaUpdate {
        @NotBlank(message = "El RUC es obligatorio") private String ruc;
        @NotBlank(message = "La razón social es obligatoria") private String razonSocial;
        private String razonComercial;
        private String contactoNombre;
        private String contactoTelefono;
        private String contactoCorreo;
        private String estado;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DetalleOrdenCompraDto {
        private Long idDetalleOc;
        private Long idMaterial;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private String codigoMaterial;
        private String nombreMaterial;
        private String categoria;
        private String unidadMedida;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrdenCompraDto {
        private Long idOrdenCompra;
        private String codigo;
        private String proveedor;
        private Long idEmpresa;
        private String rucProveedor;
        private LocalDate fechaEmision;
        private String estado;
        private List<DetalleOrdenCompraDto> items;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrdenCompraCreate {
        @NotBlank(message = "El código es obligatorio") private String codigo;
        private String proveedor;
        private Long idEmpresa;
        @NotNull(message = "La fecha de emisión es obligatoria") private LocalDate fechaEmision;
        private List<DetalleOrdenCompraDto> items;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrdenCompraUpdate {
        @NotBlank(message = "El código es obligatorio") private String codigo;
        private String proveedor;
        private Long idEmpresa;
        @NotNull(message = "La fecha de emisión es obligatoria") private LocalDate fechaEmision;
        private List<DetalleOrdenCompraDto> items;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IngresoItem {
        @NotBlank(message = "El código del material es obligatorio") private String codigo;
        private Integer cantidad; // opcional cuando se proveen numeroSeries
        // Informe: "Número de serie único por cada producto". Una serie por unidad ingresada.
        private List<String> numeroSeries;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IngresoRequest {
        @NotNull(message = "La orden de compra es obligatoria") private Long idOrdenCompra;
        @NotNull(message = "Los ítems son obligatorios") private List<IngresoItem> items;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IngresoResponse {
        private boolean exito;
        private String mensaje;
        private List<String> detalle;
        private List<String> materialesNoRegistrados;
    }

    // Trazabilidad individual por número de serie (informe)
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MaterialInstanciaDto {
        private Long idInstancia;
        private String numeroSerie;
        private Long idMaterial;
        private String codigoMaterial;
        private String nombreMaterial;
        private String modelo;
        private String marca;
        private String categoria;
        private Long idOrdenCompra;
        private String codigoOC;
        private String fechaIngreso;
        private String estado;
    }

    // ===== M2 · Producción =====
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProyectoDto {
        private Long idProyecto;
        private String codigo;
        private String cliente;
        private Long idEmpresa;
        private String rucCliente;
        private LocalDate fechaInicio;
        private BigDecimal presupuesto;
        private String estado;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProyectoCreate {
        @NotBlank(message = "El código es obligatorio") private String codigo;
        private String cliente;
        private Long idEmpresa;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @NotNull(message = "La fecha de inicio es obligatoria") private LocalDate fechaInicio;
        @NotNull(message = "El presupuesto es obligatorio") private BigDecimal presupuesto;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DetalleSolicitudDto {
        private Long idDetalleSolicitud;
        private Long idMaterial;
        private String codigoMaterial;
        private String nombreMaterial;
        private Integer cantidadSolicitada;
        private Integer cantidadDespachada;
        private Integer saldoPendiente;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SolicitudDto {
        private Long idSolicitud;
        private Long idProyecto;
        private String codigoProyecto;
        private String cliente;
        private Long idUsuarioSolicitante;
        private String solicitante;
        private Long idUsuarioAprobador;
        private LocalDateTime fechaSolicitud;
        private BigDecimal costoTotal;
        private BigDecimal presupuesto;
        private String estado;
        private Boolean anulada;
        private String motivoAnulacion;
        private Boolean editable;
        private List<DetalleSolicitudDto> detalle;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SolicitudAnulacionRequest {
        @NotBlank(message = "El motivo de anulación es obligatorio") private String motivo;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DetalleSolicitudCreate {
        @NotNull(message = "El material es obligatorio") private Long idMaterial;
        @NotNull(message = "La cantidad es obligatoria") private Integer cantidad;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SolicitudCreate {
        @NotNull(message = "El proyecto es obligatorio") private Long idProyecto;
        @NotNull(message = "El detalle es obligatorio") private List<DetalleSolicitudCreate> bom;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FaltanteDto {
        private Long idMaterial;
        private String codigo;
        private String nombre;
        private Integer cantidadSolicitada;
        private Integer stockActual;
        private Integer faltante;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AprobacionRequest {
        @NotBlank(message = "La decisión es obligatoria") private String decision; // APROBAR / RECHAZAR
        private String observacion;
    }

    // ===== M3 · Despacho =====
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BandejaItemDto {
        private Long idSolicitud;
        private String codigoProyecto;
        private String cliente;
        private LocalDateTime fechaSolicitud;
        private Integer totalPendiente;
        private boolean esParcial;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DespachoItemRequest {
        @NotNull(message = "El detalle de solicitud es obligatorio") private Long idDetalleSolicitud;
        @NotBlank(message = "El código digitado es obligatorio") private String codigoDigitado;
        @NotNull(message = "La cantidad entregada es obligatoria") private Integer cantidadEntregada;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DespachoRequest {
        @NotNull(message = "La solicitud es obligatoria") private Long idSolicitud;
        @NotNull(message = "Los ítems son obligatorios") private List<DespachoItemRequest> items;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ValidacionItemRequest {
        @NotNull private Long idDetalleSolicitud;
        @NotBlank private String codigoDigitado;
        @NotNull private Integer cantidadEntregada;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ValidacionResponse {
        private boolean valido;
        private String mensaje;
        private String codigoEsperado;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DespachoDto {
        private Long idDespacho;
        private Long idSolicitud;
        private String codigoProyecto;
        private String tipo;
        private String estado;
        private Boolean anulado;
        private String motivoAnulacion;
        private LocalDateTime fechaDespacho;
        private List<ItemDespachoDto> items;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ItemDespachoDto {
        private Long idDetalleDespacho;
        private Long idDetalleSolicitud;
        private String codigoDigitado;
        private String codigoEsperado;
        private Integer cantidadEntregada;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PendienteConfirmacionDto {
        private Long idDespacho;
        private Long idSolicitud;
        private String codigoProyecto;
        private LocalDateTime fechaDespacho;
        private String tipo;
        private Integer totalEntregado;
    }

    // ===== M4 · Calidad =====
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ParametroPruebaDto {
        private Long idParametro;
        private String tipoPrueba;
        private String unidad;
        private BigDecimal valorMinimo;
        private BigDecimal valorMaximo;
        private String normaReferencia;
        private Boolean activo;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ParametroCreate {
        @NotBlank(message = "El tipo de prueba es obligatorio") private String tipoPrueba;
        @NotBlank(message = "La unidad es obligatoria") private String unidad;
        @NotNull(message = "El valor mínimo es obligatorio") private BigDecimal valorMinimo;
        @NotNull(message = "El valor máximo es obligatorio") private BigDecimal valorMaximo;
        private String normaReferencia;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ParametroUpdate {
        @NotBlank(message = "El tipo de prueba es obligatorio") private String tipoPrueba;
        @NotBlank(message = "La unidad es obligatoria") private String unidad;
        @NotNull private BigDecimal valorMinimo;
        @NotNull private BigDecimal valorMaximo;
        private String normaReferencia;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PruebaCalidadCreate {
        @NotNull(message = "El proyecto es obligatorio") private Long idProyecto;
        @NotNull(message = "El parámetro es obligatorio") private Long idParametro;
        @NotNull(message = "El valor medido es obligatorio") private BigDecimal valorMedido;
        private String observacion;
        private String tecnicoExterno;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PruebaCalidadDto {
        private Long idPrueba;
        private Long idProyecto;
        private Long idParametro;
        private String codigoProyecto;
        private String tipoPrueba;
        private String unidad;
        private BigDecimal valorMinimo;
        private BigDecimal valorMaximo;
        private BigDecimal valorMedido;
        private String resultado;
        private String observacion;
        private String inspector;
        private String tecnicoExterno;
        private LocalDateTime fechaPrueba;
        private Boolean anulada;
        private String motivoAnulacion;
    }

    // ===== M5 · Mermas y devoluciones =====
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DevolucionCreate {
        @NotNull(message = "El proyecto es obligatorio") private Long idProyecto;
        @NotNull(message = "El material es obligatorio") private Long idMaterial;
        @NotNull(message = "La cantidad es obligatoria") private Integer cantidad;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MarcarMermaRequest {
        @NotBlank(message = "El motivo es obligatorio") private String motivo;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MermaDevolucionDto {
        private Long idMerma;
        private Long idProyecto;
        private String codigoProyecto;
        private Long idMaterial;
        private String codigoMaterial;
        private String nombreMaterial;
        private String tipo;
        private Integer cantidad;
        private String motivo;
        private String estado;
        private Boolean anulada;
        private String motivoAnulacion;
        private String registra;
        private String validador;
        private LocalDateTime fecha;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReporteMermaItem {
        private String codigoProyecto;
        private String codigoMaterial;
        private String nombreMaterial;
        private String motivo;
        private Integer cantidad;
        private java.math.BigDecimal costoEstimado;
    }

    // ===== M6 · Trazabilidad =====
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TrazabilidadDto {
        private Long idTrazabilidad;
        private String codigoMaterial;
        private String nombreMaterial;
        private String tipoMovimiento;
        private Integer cantidad;
        private String usuario;
        private String codigoProyecto;
        private String codigoOrdenCompra;
        private LocalDateTime fechaMovimiento;
    }
}
