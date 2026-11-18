package com.smartpoweron.gestion_materiales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class Dtos {

    // --- Autenticación y Usuarios ---
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        private String correo;
        private String password;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoginResponse {
        private String token;
        private Long idUsuario;      // <-- Añadido
        private String username;
        private String nombreCompleto; // <-- Añadido
        private String rol;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UsuarioDto {
        private Long idUsuario;
        private String username;
        private String nombreCompleto;
        private String correo;
        private Long idRol;          // <-- Añadido
        private String rol;
        private Boolean activo;
        private LocalDateTime ultimoAcceso; // <-- Añadido
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UsuarioCreate {
        private String username;
        private String password;
        private String nombreCompleto;
        private String correo;
        private Long idRol;
        private Boolean activo;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UsuarioUpdate {
        private String password;
        private String nombreCompleto;
        private String correo;
        private Long idRol;
        private Boolean activo;
    }

    // --- Empresas ---
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EmpresaDto {
        private Long idEmpresa;
        private String ruc;
        private String razonSocial;
        private String razonComercial;
        private String tipo;
        private String contactoNombre;
        private String contactoTelefono;
        private String contactoCorreo;
        private String estado;
        private LocalDate fechaRegistro;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EmpresaCreate {
        private String ruc;
        private String razonSocial;
        private String razonComercial;
        private String tipo;         // <-- Añadido
        private String contactoNombre;
        private String contactoTelefono;
        private String contactoCorreo;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EmpresaUpdate {
        private String ruc;
        private String razonSocial;
        private String razonComercial;
        private String tipo;
        private String contactoNombre;
        private String contactoTelefono;
        private String contactoCorreo;
        private String estado;
    }

    // --- Materiales ---
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MaterialDto {
        private Long idMaterial;
        private String codigo;
        private String nombre;
        private String descripcion;
        private String categoria;
        private String marca;
        private String modelo;
        private String mlfb;
        private String unidadMedida;
        private String especificacionTecnica;
        private BigDecimal precioUnitario;
        private Integer stockActual;
        private Integer stockMinimo;
        private String estado;
        private String pasillo;
        private String estante;
        private String nivel;
        private boolean enAlerta;    // <-- Añadido
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MaterialCreate {
        private String codigo;
        private String nombre;
        private String descripcion;
        private String categoria;
        private String marca;
        private String modelo;
        private String mlfb;
        private String unidadMedida;
        private String especificacionTecnica;
        private BigDecimal precioUnitario;
        private Integer stockMinimo;
        private String pasillo;
        private String estante;
        private String nivel;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MaterialUpdate {
        private String codigo;
        private String nombre;
        private String descripcion;
        private String categoria;
        private String marca;
        private String modelo;
        private String mlfb;
        private String unidadMedida;
        private String especificacionTecnica;
        private BigDecimal precioUnitario;
        private Integer stockMinimo;
        private String pasillo;
        private String estante;
        private String nivel;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MaterialInstanciaDto {
        private Long idInstancia;
        private String numeroSerie;
        private Long idMaterial;
        private String codigoMaterial;   // <-- Añadido
        private String nombreMaterial;   // <-- Añadido
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoriaStockDto {
        private String categoria;
        private Integer stockTotal;
        private Integer stockMinimoTotal; // <-- Añadido
        private Integer numMateriales;
        private BigDecimal valorInventario; // <-- Añadido
    }

    // --- Órdenes de Compra ---
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrdenCompraDto {
        private Long idOrdenCompra;
        private String codigo;
        private String proveedor;
        private String rucProveedor;
        private Long idEmpresa;
        private LocalDate fechaEmision;
        private String estado;
        private List<DetalleOrdenCompraDto> items; // <-- Añadido
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrdenCompraCreate {
        private String codigo;
        private String proveedor;
        private Long idEmpresa;
        private LocalDate fechaEmision;
        private List<DetalleOrdenCompraDto> items;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrdenCompraUpdate {
        private String codigo;
        private String proveedor;
        private Long idEmpresa;
        private LocalDate fechaEmision;
        private List<DetalleOrdenCompraDto> items;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DetalleOrdenCompraDto {
        private Long idDetalle;
        private Long idDetalleOc;      // <-- Añadido
        private Long idMaterial;
        private String codigoMaterial;  // <-- Añadido
        private Integer cantidad;
        private BigDecimal precioUnitario;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IngresoRequest {
        private Long idOrdenCompra;
        private List<IngresoItem> items;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IngresoItem {
        private String codigo;
        private Integer cantidad;
        private List<String> numeroSeries;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IngresoResponse {
        private boolean exito;
        private String mensaje;
        private Integer nuevoStock;
        private List<String> detalle;
        private List<String> materialesNoRegistrados; // <-- Añadido
    }

    // --- Proyectos y Solicitudes ---
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProyectoDto {
        private Long idProyecto;
        private String codigo;
        private String cliente;
        private String rucCliente; // <-- Añadido
        private Long idEmpresa;
        private LocalDate fechaInicio;
        private Double presupuesto;
        private String estado;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProyectoCreate {
        private String codigo;
        private String cliente;
        private Long idEmpresa;
        private LocalDate fechaInicio;
        private Double presupuesto; // Ojo: el test usa BigDecimal, pero el servicio parece usar Double
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SolicitudDto {
        private Long idSolicitud;
        private Long idProyecto;
        private String codigoProyecto;
        private String cliente;            // <-- Añadido
        private Long idUsuarioSolicitante; // <-- Añadido
        private String estado;
        private Boolean anulada;
        private List<DetalleSolicitudDto> detalle;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SolicitudCreate {
        private Long idProyecto;
        private List<DetalleSolicitudCreate> bom;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DetalleSolicitudCreate {
        private Long idMaterial;
        private Integer cantidad;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DetalleSolicitudDto {
        private Long idDetalle;
        private Long idDetalleSolicitud;  // <-- Añadido
        private Long idMaterial;
        private String codigoMaterial;     // <-- Añadido
        private Integer cantidad;
        private Integer cantidadSolicitada; // <-- Añadido
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AprobacionRequest {
        private Boolean aprobada;
        private String observacion;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FaltanteDto {
        private Long idMaterial;
        private String codigo;
        private String nombre;            // <-- Añadido
        private String codigoMaterial;
        private Integer cantidadFaltante;
    }

    // --- Calidad ---
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PruebaCalidadDto {
        private Long idPrueba;
        private Long idProyecto;
        private String codigoProyecto;
        private Long idParametro;
        private String tipoPrueba;   // <-- Añadido
        private BigDecimal valorMedido;
        private String estado;
        private Boolean anulada;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PruebaCalidadCreate {
        private Long idProyecto;
        private Long idParametro;
        private BigDecimal valorMedido;
        private String observacion;
        private String tecnicoExterno;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ParametroPruebaDto {
        private Long idParametro;
        private String nombre;
        private String tipoPrueba;
        private String unidad;         // <-- Añadido
        private BigDecimal valorMinimo; // <-- Añadido
        private BigDecimal valorMaximo; // <-- Añadido
        private Boolean activo;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ParametroCreate {
        private String nombre;
        private String tipoPrueba;
        private String unidad;
        private BigDecimal valorMinimo;
        private BigDecimal valorMaximo;
        private String normaReferencia;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ParametroUpdate {
        private String nombre;
        private String tipoPrueba;
        private String unidad;
        private BigDecimal valorMinimo;
        private BigDecimal valorMaximo;
        private String normaReferencia;
    }

    // --- Despacho ---
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BandejaItemDto {
        private Long idSolicitud;
        private String codigoProyecto;
        private String cliente;
        private LocalDateTime fechaSolicitud; // <-- Añadido
        private String estado;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ValidacionResponse {
        private Boolean valido;
        private String mensaje;
        private String codigoEsperado;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ValidacionItemRequest {
        private Long idDetalleSolicitud;
        private String codigoDigitado;
        private Integer cantidadEntregada;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DespachoDto {
        private Long idDespacho;
        private Long idSolicitud;
        private String codigoProyecto;
        private String tipo;
        private String estado;
        private Boolean anulado;   // <-- Añadido
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DespachoRequest {
        private Long idSolicitud;
        private List<DespachoItemRequest> items;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DespachoItemRequest {
        private Long idDetalleSolicitud;
        private String codigoDigitado;
        private Integer cantidadEntregada;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PendienteConfirmacionDto {
        private Long idDespacho;
        private Long idSolicitud;
        private String codigoProyecto;
        private String proyecto;
        private LocalDateTime fechaDespacho; // <-- Añadido
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ItemDespachoDto {
        private Long idDetalle;
        private Long idDetalleDespacho;
        private Long idDetalleSolicitud; // <-- Añadido
        private String codigoMaterial;
        private Integer cantidad;
    }

    // --- Mermas y Devoluciones ---
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SolicitudAnulacionRequest {
        private Long id;
        private String motivo;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DevolucionCreate {
        private Long idProyecto;
        private Long idMaterial;
        private Integer cantidad;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MarcarMermaRequest {
        private String motivo;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReporteMermaItem {
        private String codigoProyecto;
        private String codigoMaterial;
        private String nombreMaterial;
        private String motivo;
        private Integer cantidad;
        private BigDecimal costoEstimado;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MermaDevolucionDto {
        private Long idMerma;
        private Long idProyecto;
        private String codigoProyecto;
        private Long idMaterial;
        private String codigoMaterial;
        private String nombreMaterial;
        private String tipo; // <-- Añadido
    }

    // --- Trazabilidad ---
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TrazabilidadDto {
        private Long idTrazabilidad;
        private String codigoMaterial;
        private String nombreMaterial;
        private String tipoMovimiento;
        private String accion;
        private Integer cantidad; // <-- Añadido
    }
}