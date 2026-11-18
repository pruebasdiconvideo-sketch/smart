package com.smartpoweron.gestion_materiales.controller.api;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API de autenticación (A0, RF01, RF04).
 *  A01 POST /api/auth/login   (público)
 *  A02 POST /api/auth/logout  (todos)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthService authService;

    public AuthApiController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login")
    public ResponseEntity<Dtos.LoginResponse> login(@Valid @RequestBody Dtos.LoginRequest req,
                                                     HttpServletResponse res) {
        return ResponseEntity.ok(authService.autenticar(req, res));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse res) {
        authService.cerrarSesion(res);
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente."));
    }
}
