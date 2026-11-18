package com.smartpoweron.gestion_materiales.controller.api;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.service.CalidadService;
import com.smartpoweron.gestion_materiales.service.CertificadoService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de calidad y certificado (M4, RF25-RF29).
 *  A27 POST /api/calidad/pruebas
 *  A28 GET  /api/calidad/pruebas
 *  A29 GET  /api/calidad/proyectos/{id}/certificado.pdf
 */
@RestController
@RequestMapping("/api/calidad")
public class CalidadApiController {

    private final CalidadService calidadService;
    private final CertificadoService certificadoService;

    public CalidadApiController(CalidadService calidadService, CertificadoService certificadoService) {
        this.calidadService = calidadService;
        this.certificadoService = certificadoService;
    }

    // RF25, RF26, RF27 - registrar prueba (resultado calculado en el servidor)
    @PostMapping("/pruebas")
    public ResponseEntity<Dtos.PruebaCalidadDto> registrarPrueba(@Valid @RequestBody Dtos.PruebaCalidadCreate req) {
        return ResponseEntity.ok(calidadService.registrarPrueba(req));
    }

    // RF27, RF29 - historial por proyecto
    @GetMapping("/pruebas")
    public ResponseEntity<List<Dtos.PruebaCalidadDto>> listarPruebas(@RequestParam Long proyecto) {
        return ResponseEntity.ok(calidadService.listarPorProyecto(proyecto));
    }

    @PutMapping("/pruebas/{id}")
    public ResponseEntity<Dtos.PruebaCalidadDto> actualizarPrueba(@PathVariable Long id,
            @Valid @RequestBody Dtos.PruebaCalidadCreate req) {
        return ResponseEntity.ok(calidadService.actualizarPrueba(id, req));
    }

    @DeleteMapping("/pruebas/{id}")
    public ResponseEntity<Void> anularPrueba(@PathVariable Long id,
            @Valid @RequestBody Dtos.SolicitudAnulacionRequest req) {
        calidadService.anularPrueba(id, req.getMotivo());
        return ResponseEntity.noContent().build();
    }

    // RF28 - certificado PDF (sólo si la última medición de cada parámetro es CONFORME)
    @GetMapping(value = "/proyectos/{id}/certificado.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> certificado(@PathVariable Long id) {
        byte[] pdf = certificadoService.generarCertificado(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition",
                        "attachment; filename=certificado_proyecto_" + id + ".pdf")
                .body(pdf);
    }
}
