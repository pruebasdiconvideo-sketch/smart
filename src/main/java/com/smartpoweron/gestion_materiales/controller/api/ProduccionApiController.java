package com.smartpoweron.gestion_materiales.controller.api;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.service.ProyectoService;
import com.smartpoweron.gestion_materiales.service.SolicitudService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de producción / solicitudes (M2, RF12-RF18).
 *  A15 GET / POST /api/produccion/proyectos
 *  A16 POST /api/produccion/solicitudes
 *  A17 GET  /api/produccion/solicitudes
 *  A18 POST /api/produccion/solicitudes/{id}/aprobar
 *  A19 POST /api/produccion/solicitudes/{id}/rechazar
 *  A20 POST /api/produccion/solicitudes/{id}/verificar-stock
 *  A21 GET  /api/produccion/solicitudes/{id}/faltantes
 */
@RestController
@RequestMapping("/api/produccion")
public class ProduccionApiController {

    private final ProyectoService proyectoService;
    private final SolicitudService solicitudService;

    public ProduccionApiController(ProyectoService proyectoService, SolicitudService solicitudService) {
        this.proyectoService = proyectoService;
        this.solicitudService = solicitudService;
    }

    // RF12 - listar / crear proyectos
    @GetMapping("/proyectos")
    public ResponseEntity<List<Dtos.ProyectoDto>> listarProyectos() {
        return ResponseEntity.ok(proyectoService.listarProyectos());
    }

    @PostMapping("/proyectos")
    public ResponseEntity<Dtos.ProyectoDto> crearProyecto(@Valid @RequestBody Dtos.ProyectoCreate req) {
        return ResponseEntity.ok(proyectoService.crearProyecto(req));
    }

    @PutMapping("/proyectos/{id}")
    public ResponseEntity<Dtos.ProyectoDto> actualizarProyecto(
            @PathVariable Long id, @Valid @RequestBody Dtos.ProyectoCreate req) {
        return ResponseEntity.ok(proyectoService.actualizarProyecto(id, req));
    }

    @DeleteMapping("/proyectos/{id}")
    public ResponseEntity<Void> anularProyecto(@PathVariable Long id) {
        proyectoService.anularProyecto(id);
        return ResponseEntity.noContent().build();
    }

    // RF13-RF15 - registrar solicitud con BOM
    @PostMapping("/solicitudes")
    public ResponseEntity<Dtos.SolicitudDto> registrarSolicitud(@Valid @RequestBody Dtos.SolicitudCreate req) {
        return ResponseEntity.ok(solicitudService.registrarConBom(req));
    }

    @PutMapping("/solicitudes/{id}")
    public ResponseEntity<Dtos.SolicitudDto> actualizarSolicitud(@PathVariable Long id,
            @Valid @RequestBody Dtos.SolicitudCreate req) {
        return ResponseEntity.ok(solicitudService.actualizar(id, req));
    }

    @DeleteMapping("/solicitudes/{id}")
    public ResponseEntity<Dtos.SolicitudDto> anularSolicitud(@PathVariable Long id,
            @Valid @RequestBody Dtos.SolicitudAnulacionRequest req) {
        return ResponseEntity.ok(solicitudService.anular(id, req));
    }

    // RF15, RF16 - listar por estado
    @GetMapping("/solicitudes")
    public ResponseEntity<List<Dtos.SolicitudDto>> listarSolicitudes(
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(solicitudService.listarPorEstado(estado));
    }

    // RF16 - aprobar
    @PostMapping("/solicitudes/{id}/aprobar")
    public ResponseEntity<Dtos.SolicitudDto> aprobar(@PathVariable Long id,
                                                      @RequestBody(required = false) Dtos.AprobacionRequest req) {
        return ResponseEntity.ok(solicitudService.aprobar(id, req));
    }

    // RF16 - rechazar
    @PostMapping("/solicitudes/{id}/rechazar")
    public ResponseEntity<Dtos.SolicitudDto> rechazar(@PathVariable Long id,
                                                       @RequestBody(required = false) Dtos.AprobacionRequest req) {
        return ResponseEntity.ok(solicitudService.rechazar(id, req));
    }

    // RF17 - verificar stock
    @PostMapping("/solicitudes/{id}/verificar-stock")
    public ResponseEntity<Dtos.SolicitudDto> verificarStock(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.verificarStock(id));
    }

    // RF18 - listar faltantes
    @GetMapping("/solicitudes/{id}/faltantes")
    public ResponseEntity<List<Dtos.FaltanteDto>> faltantes(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.listarFaltantes(id));
    }
}
