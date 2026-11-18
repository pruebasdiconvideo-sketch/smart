package com.smartpoweron.gestion_materiales.controller.api;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.service.TrazabilidadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de trazabilidad (M6, RF36).
 *  A35 GET /api/trazabilidad/materiales/{codigo}
 */
@RestController
@RequestMapping("/api/trazabilidad")
public class TrazabilidadApiController {

    private final TrazabilidadService trazabilidadService;

    public TrazabilidadApiController(TrazabilidadService trazabilidadService) {
        this.trazabilidadService = trazabilidadService;
    }

    @GetMapping("/materiales/{codigo}")
    public ResponseEntity<List<Dtos.TrazabilidadDto>> consultar(@PathVariable String codigo) {
        return ResponseEntity.ok(trazabilidadService.consultarPorMaterial(codigo));
    }
}
