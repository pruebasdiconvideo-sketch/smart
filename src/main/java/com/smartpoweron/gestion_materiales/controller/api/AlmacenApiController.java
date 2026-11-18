package com.smartpoweron.gestion_materiales.controller.api;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de inventario y catálogo (M1, RF05-RF11).
 *  A09 GET   /api/almacen/materiales
 *  A10 POST  /api/almacen/materiales
 *  A11 PUT   /api/almacen/materiales/{id}
 *  A12 GET   /api/almacen/ordenes-compra
 *  A13 POST  /api/almacen/ingresos
 *  A14 GET   /api/almacen/alertas-stock
 */
@RestController
@RequestMapping("/api/almacen")
public class AlmacenApiController {

    private final MaterialService materialService;

    public AlmacenApiController(MaterialService materialService) {
        this.materialService = materialService;
    }

    // RF06 - consultar catálogo con filtros
    @GetMapping("/materiales")
    public ResponseEntity<List<Dtos.MaterialDto>> consultarCatalogo(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return ResponseEntity.ok(materialService.consultarCatalogo(codigo, nombre, categoria, incluirInactivos));
    }

    // RF05 - registrar material
    @PostMapping("/materiales")
    public ResponseEntity<Dtos.MaterialDto> registrarMaterial(@Valid @RequestBody Dtos.MaterialCreate req) {
        return ResponseEntity.ok(materialService.registrarMaterial(req));
    }

    // RF07 - actualizar material
    @PutMapping("/materiales/{id}")
    public ResponseEntity<Dtos.MaterialDto> actualizarMaterial(@PathVariable Long id,
                                                                @Valid @RequestBody Dtos.MaterialUpdate req) {
        return ResponseEntity.ok(materialService.actualizarMaterial(id, req));
    }

    @DeleteMapping("/materiales/{id}")
    public ResponseEntity<Void> desactivarMaterial(@PathVariable Long id) {
        materialService.desactivarMaterial(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/materiales/{id}/reactivar")
    public ResponseEntity<Void> reactivarMaterial(@PathVariable Long id) {
        materialService.reactivarMaterial(id);
        return ResponseEntity.noContent().build();
    }

    // RF08 - listar órdenes de compra abiertas
    @GetMapping("/ordenes-compra")
    public ResponseEntity<List<Dtos.OrdenCompraDto>> listarOrdenes(
            @RequestParam(defaultValue = "false") boolean incluirCerradas) {
        return ResponseEntity.ok(materialService.listarOrdenes(incluirCerradas));
    }

    @PostMapping("/ordenes-compra")
    public ResponseEntity<Dtos.OrdenCompraDto> crearOrden(@Valid @RequestBody Dtos.OrdenCompraCreate req) {
        return ResponseEntity.ok(materialService.crearOrden(req));
    }

    @PutMapping("/ordenes-compra/{id}")
    public ResponseEntity<Dtos.OrdenCompraDto> actualizarOrden(@PathVariable Long id,
            @Valid @RequestBody Dtos.OrdenCompraUpdate req) {
        return ResponseEntity.ok(materialService.actualizarOrden(id, req));
    }

    @DeleteMapping("/ordenes-compra/{id}")
    public ResponseEntity<Void> cerrarOrden(@PathVariable Long id) {
        materialService.cerrarOrden(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/ordenes-compra/{id}/reabrir")
    public ResponseEntity<Void> reabrirOrden(@PathVariable Long id) {
        materialService.reabrirOrden(id);
        return ResponseEntity.noContent().build();
    }

    // RF08, RF09, RF10 - registrar ingreso contra OC
    @PostMapping("/ingresos")
    public ResponseEntity<Dtos.IngresoResponse> registrarIngreso(@Valid @RequestBody Dtos.IngresoRequest req) {
        return ResponseEntity.ok(materialService.registrarIngreso(req));
    }

    // RF11 - alertas de stock mínimo
    @GetMapping("/alertas-stock")
    public ResponseEntity<List<Dtos.MaterialDto>> alertasStock() {
        return ResponseEntity.ok(materialService.listarAlertas());
    }

    // Stock agrupado por categoria
    @GetMapping("/categorias")
    public ResponseEntity<List<Dtos.CategoriaStockDto>> stockPorCategoria() {
        return ResponseEntity.ok(materialService.stockPorCategoria());
    }

    // Lista de categorias disponibles (para dropdowns de filtros)
    @GetMapping("/categorias-lista")
    public ResponseEntity<List<String>> categoriasDisponibles() {
        return ResponseEntity.ok(materialService.categoriasDisponibles());
    }

    // Trazabilidad individual por número de serie (informe).
    // Uso: GET /api/almacen/instancias?serie=SN-001  → una instancia
    //      GET /api/almacen/instancias?material={id} → listado por material
    @GetMapping("/instancias")
    public ResponseEntity<?> instancias(
            @RequestParam(required = false) String serie,
            @RequestParam(required = false) Long material) {
        if (serie != null && !serie.isBlank()) {
            Dtos.MaterialInstanciaDto dto = materialService.buscarInstanciaPorSerie(serie);
            return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
        }
        if (material != null) {
            return ResponseEntity.ok(materialService.listarInstanciasPorMaterial(material));
        }
        return ResponseEntity.badRequest().build();
    }

    // Preview del proximo codigo automatico segun categoria
    @GetMapping("/materiales/siguiente-codigo")
    public ResponseEntity<java.util.Map<String, String>> siguienteCodigo(@RequestParam String categoria) {
        return ResponseEntity.ok(java.util.Map.of("codigo", materialService.siguienteCodigo(categoria)));
    }
}
