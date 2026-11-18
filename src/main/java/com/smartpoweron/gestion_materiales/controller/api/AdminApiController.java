package com.smartpoweron.gestion_materiales.controller.api;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.service.CalidadService;
import com.smartpoweron.gestion_materiales.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API de administración (M0/M4, RF03, RF30).
 *  A03 GET    /api/admin/usuarios
 *  A04 POST   /api/admin/usuarios
 *  A05 PUT    /api/admin/usuarios/{id}
 *  A06 PATCH  /api/admin/usuarios/{id}/desactivar
 *  A07 GET    /api/admin/parametros-prueba
 *  A08 PUT    /api/admin/parametros-prueba/{id}
 */
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final UsuarioService usuarioService;
    private final CalidadService calidadService;

    public AdminApiController(UsuarioService usuarioService, CalidadService calidadService) {
        this.usuarioService = usuarioService;
        this.calidadService = calidadService;
    }

    // --- Usuarios ---
    @GetMapping("/usuarios")
    public ResponseEntity<List<Dtos.UsuarioDto>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @PostMapping("/usuarios")
    public ResponseEntity<Dtos.UsuarioDto> crearUsuario(@Valid @RequestBody Dtos.UsuarioCreate req) {
        return ResponseEntity.ok(usuarioService.crear(req));
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<Dtos.UsuarioDto> actualizarUsuario(@PathVariable Long id,
                                                              @Valid @RequestBody Dtos.UsuarioUpdate req) {
        return ResponseEntity.ok(usuarioService.actualizar(id, req));
    }

    @PatchMapping("/usuarios/{id}/desactivar")
    public ResponseEntity<Map<String, String>> desactivarUsuario(@PathVariable Long id) {
        usuarioService.desactivar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario desactivado."));
    }

    @PatchMapping("/usuarios/{id}/reactivar")
    public ResponseEntity<Map<String, String>> reactivarUsuario(@PathVariable Long id) {
        usuarioService.reactivar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario reactivado."));
    }

    // --- Parámetros de prueba ---
    @GetMapping("/parametros-prueba")
    public ResponseEntity<List<Dtos.ParametroPruebaDto>> listarParametros(
            @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return ResponseEntity.ok(calidadService.listarParametros(incluirInactivos));
    }

    @PostMapping("/parametros-prueba")
    public ResponseEntity<Dtos.ParametroPruebaDto> crearParametro(@Valid @RequestBody Dtos.ParametroCreate req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(calidadService.crearParametro(req));
    }

    @PutMapping("/parametros-prueba/{id}")
    public ResponseEntity<Dtos.ParametroPruebaDto> configurarRango(@PathVariable Long id,
                                                                    @Valid @RequestBody Dtos.ParametroUpdate req) {
        return ResponseEntity.ok(calidadService.configurarRango(id, req));
    }

    @DeleteMapping("/parametros-prueba/{id}")
    public ResponseEntity<Void> desactivarParametro(@PathVariable Long id) {
        calidadService.desactivarParametro(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/parametros-prueba/{id}/reactivar")
    public ResponseEntity<Void> reactivarParametro(@PathVariable Long id) {
        calidadService.reactivarParametro(id);
        return ResponseEntity.noContent().build();
    }
}
