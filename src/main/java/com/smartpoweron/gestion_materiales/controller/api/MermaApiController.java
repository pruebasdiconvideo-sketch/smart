package com.smartpoweron.gestion_materiales.controller.api;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.service.MermaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de mermas y devoluciones (M5, RF31-RF35).
 *  A30 POST /api/merma/devoluciones
 *  A31 POST /api/merma/devoluciones/{id}/reintegrar
 *  A32 POST /api/merma/devoluciones/{id}/marcar-merma
 *  A33 POST /api/merma/devoluciones/{id}/validar
 *  A34 GET  /api/merma/reporte
 */
@RestController
@RequestMapping("/api/merma")
public class MermaApiController {

    private final MermaService mermaService;

    public MermaApiController(MermaService mermaService) {
        this.mermaService = mermaService;
    }

    // RF31 - operario registra devolución
    @PostMapping("/devoluciones")
    public ResponseEntity<Dtos.MermaDevolucionDto> registrarDevolucion(@Valid @RequestBody Dtos.DevolucionCreate req) {
        return ResponseEntity.ok(mermaService.registrarDevolucion(req));
    }

    @PutMapping("/devoluciones/{id}")
    public ResponseEntity<Dtos.MermaDevolucionDto> actualizarDevolucion(@PathVariable Long id,
            @Valid @RequestBody Dtos.DevolucionCreate req) {
        return ResponseEntity.ok(mermaService.actualizar(id, req));
    }

    @DeleteMapping("/devoluciones/{id}")
    public ResponseEntity<Void> anularDevolucion(@PathVariable Long id,
            @Valid @RequestBody Dtos.SolicitudAnulacionRequest req) {
        mermaService.anular(id, req.getMotivo());
        return ResponseEntity.noContent().build();
    }

    // RF32 - almacenero reintegra
    @PostMapping("/devoluciones/{id}/reintegrar")
    public ResponseEntity<Dtos.MermaDevolucionDto> reintegrar(@PathVariable Long id) {
        return ResponseEntity.ok(mermaService.reintegrar(id));
    }

    // RF33 - almacenero marca merma
    @PostMapping("/devoluciones/{id}/marcar-merma")
    public ResponseEntity<Dtos.MermaDevolucionDto> marcarMerma(@PathVariable Long id,
                                                                @Valid @RequestBody Dtos.MarcarMermaRequest req) {
        return ResponseEntity.ok(mermaService.marcarMerma(id, req));
    }

    // RF34 - jefe de producción valida baja
    @PostMapping("/devoluciones/{id}/validar")
    public ResponseEntity<Dtos.MermaDevolucionDto> validar(@PathVariable Long id) {
        return ResponseEntity.ok(mermaService.validarBaja(id));
    }

    // RF35 - reporte de mermas
    @GetMapping("/reporte")
    public ResponseEntity<List<Dtos.ReporteMermaItem>> reporte(
            @RequestParam(required = false) Long proyecto,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        return ResponseEntity.ok(mermaService.reporteMermas(proyecto, desde, hasta));
    }

    // Consulta auxiliar para la vista V12 (no cuenta entre los 35 RF-endpoints)
    @GetMapping("/devoluciones")
    public ResponseEntity<List<Dtos.MermaDevolucionDto>> listar() {
        return ResponseEntity.ok(mermaService.listarTodas());
    }
}
