package com.smartpoweron.gestion_materiales.controller.api;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.service.DespachoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de despacho y entrega (M3, RF19-RF24).
 *  A22 GET  /api/despacho/bandeja
 *  A23 GET  /api/despacho/validar
 *  A24 POST /api/despacho
 *  A25 GET  /api/despacho/pendientes-confirmacion
 *  A26 POST /api/despacho/{id}/confirmar
 */
@RestController
@RequestMapping("/api/despacho")
public class DespachoApiController {

    private final DespachoService despachoService;

    public DespachoApiController(DespachoService despachoService) {
        this.despachoService = despachoService;
    }

    // RF19 - bandeja
    @GetMapping("/bandeja")
    public ResponseEntity<List<Dtos.BandejaItemDto>> bandeja() {
        return ResponseEntity.ok(despachoService.listarBandeja());
    }

    // RF21 - validar código y cantidad antes de registrar
    @GetMapping("/validar")
    public ResponseEntity<Dtos.ValidacionResponse> validar(@RequestParam Long idDetalleSolicitud,
                                                            @RequestParam String codigoDigitado,
                                                            @RequestParam Integer cantidadEntregada) {
        Dtos.ValidacionItemRequest req = Dtos.ValidacionItemRequest.builder()
                .idDetalleSolicitud(idDetalleSolicitud)
                .codigoDigitado(codigoDigitado)
                .cantidadEntregada(cantidadEntregada)
                .build();
        return ResponseEntity.ok(despachoService.validarCodigo(req));
    }

    // RF20, RF21, RF22 - registrar despacho
    @PostMapping
    public ResponseEntity<Dtos.DespachoDto> registrarDespacho(@Valid @RequestBody Dtos.DespachoRequest req) {
        return ResponseEntity.ok(despachoService.registrarDespacho(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dtos.DespachoDto> obtenerDespacho(@PathVariable Long id) {
        return ResponseEntity.ok(despachoService.obtenerDespacho(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Dtos.DespachoDto> actualizarDespacho(@PathVariable Long id,
            @Valid @RequestBody Dtos.DespachoRequest req) {
        return ResponseEntity.ok(despachoService.actualizarDespacho(id, req));
    }

    // RF23 - bandeja del operario
    @GetMapping("/pendientes-confirmacion")
    public ResponseEntity<List<Dtos.PendienteConfirmacionDto>> pendientes() {
        return ResponseEntity.ok(despachoService.listarPendientesConfirmacion());
    }

    // RF23, RF24 - confirmar recepción
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<Dtos.DespachoDto> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(despachoService.confirmarEntrega(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Dtos.DespachoDto> anular(@PathVariable Long id,
            @Valid @RequestBody Dtos.SolicitudAnulacionRequest req) {
        return ResponseEntity.ok(despachoService.anular(id, req.getMotivo()));
    }
}
