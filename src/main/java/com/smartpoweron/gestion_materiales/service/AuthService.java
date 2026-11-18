package com.smartpoweron.gestion_materiales.service;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.entity.Rol;
import com.smartpoweron.gestion_materiales.entity.Usuario;
import com.smartpoweron.gestion_materiales.repository.RolRepository;
import com.smartpoweron.gestion_materiales.repository.UsuarioRepository;
import com.smartpoweron.gestion_materiales.security.JwtUtil;
import com.smartpoweron.gestion_materiales.security.UsuarioDetailsService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Autenticación con JWT (RF01, RF04, RNF03, RNF04).
 *
 * - autenticar: valida correo + BCrypt y, si la cuenta está activa, emite un
 *   token HS256 con idUsuario, correo y rol. La cookie HttpOnly `jwt` caduca
 *   con el token (RF04 / cierre por inactividad).
 * - cerrarSesion: limpia la cookie.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepo;
    private final RolRepository rolRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    @Value("${app.jwt.expiracion-minutos:480}")
    private long expiracionMinutos;

    public AuthService(UsuarioRepository usuarioRepo, RolRepository rolRepo,
                       PasswordEncoder encoder, JwtUtil jwtUtil,
                       UsuarioDetailsService uds) {
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public Dtos.LoginResponse autenticar(Dtos.LoginRequest req, HttpServletResponse res) {
        Usuario u = usuarioRepo.findByCorreo(req.getCorreo())
                .orElseThrow(() -> new com.smartpoweron.gestion_materiales.exception.AutenticacionException(
                        "Usuario o contraseña incorrectos"));
        if (!Boolean.TRUE.equals(u.getActivo())) {
            throw new com.smartpoweron.gestion_materiales.exception.AutenticacionException("El usuario está inactivo");
        }
        if (!encoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new com.smartpoweron.gestion_materiales.exception.AutenticacionException(
                    "Usuario o contraseña incorrectos");
        }
        Rol r = rolRepo.findById(u.getIdRol())
                .orElseThrow(() -> new com.smartpoweron.gestion_materiales.exception.AutenticacionException(
                        "El rol asignado no existe"));
        u.setUltimoAcceso(LocalDateTime.now());
        usuarioRepo.save(u);
        String token = jwtUtil.generateToken(u.getIdUsuario(), u.getCorreo(), r.getNombre());
        // Cookie HttpOnly para vistas Thymeleaf (RF04)
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (expiracionMinutos * 60));
        res.addCookie(cookie);
        return Dtos.LoginResponse.builder()
                .token(token)
                .nombreCompleto(u.getNombreCompleto())
                .rol(r.getNombre())
                .idUsuario(u.getIdUsuario())
                .build();
    }

    public void cerrarSesion(HttpServletResponse res) {
        Cookie cookie = new Cookie("jwt", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        res.addCookie(cookie);
    }

    public String extraerTokenDeCookie(HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) {
            if ("jwt".equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
