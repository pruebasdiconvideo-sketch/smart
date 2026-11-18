package com.smartpoweron.gestion_materiales.controller.api;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de empresas (proveedores y clientes).
 * Una sola tabla con discriminador tipo=PROVEEDOR|CLIENTE. El parámetro
 * ?tipo= filtra por uno de los dos. Permite registrar la empresa una sola vez
 * y reutilizarla en órdenes de compra y proyectos.
 */
@RestController
@RequestMapping("/api/empresa")
public class EmpresaApiController {

    private final EmpresaService empresaService;

    public EmpresaApiController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public ResponseEntity<List<Dtos.EmpresaDto>> listar(@RequestParam(required = false) String tipo,
                                                         @RequestParam(defaultValue = "true") boolean incluirInactivos) {
        return ResponseEntity.ok(incluirInactivos
                ? empresaService.listarTodos(tipo)
                : empresaService.listar(tipo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dtos.EmpresaDto> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(empresaService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<Dtos.EmpresaDto> registrar(@Valid @RequestBody Dtos.EmpresaCreate req) {
        return ResponseEntity.ok(empresaService.registrar(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Dtos.EmpresaDto> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody Dtos.EmpresaUpdate req) {
        return ResponseEntity.ok(empresaService.actualizar(id, req));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        empresaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivar(@PathVariable Long id) {
        empresaService.reactivar(id);
        return ResponseEntity.noContent().build();
    }
}
