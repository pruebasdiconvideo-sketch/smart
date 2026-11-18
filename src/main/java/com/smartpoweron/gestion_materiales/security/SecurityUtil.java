package com.smartpoweron.gestion_materiales.security;

import com.smartpoweron.gestion_materiales.entity.Rol;
import com.smartpoweron.gestion_materiales.entity.Usuario;
import com.smartpoweron.gestion_materiales.repository.RolRepository;
import com.smartpoweron.gestion_materiales.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Acceso al usuario autenticado (firmante del JWT) para que los servicios de
 * negocio registren al usuario en cada operación (RF03, RF10, RF24, etc.).
 */
@Component
public class SecurityUtil {

    private final UsuarioRepository usuarioRepo;
    private final RolRepository rolRepo;

    public SecurityUtil(UsuarioRepository usuarioRepo, RolRepository rolRepo) {
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
    }

    public Optional<Usuario> usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        String correo = null;
        if (principal instanceof org.springframework.security.core.userdetails.User u) {
            correo = u.getUsername();
        } else if (principal instanceof String s) {
            correo = s;
        }
        if (correo == null) return Optional.empty();
        return usuarioRepo.findByCorreo(correo);
    }

    public Usuario usuarioOExcepcion() {
        return usuarioActual().orElseThrow(() ->
                new IllegalStateException("No hay usuario autenticado en el contexto de seguridad."));
    }

    public Rol rolActual() {
        Usuario u = usuarioOExcepcion();
        return rolRepo.findById(u.getIdRol())
                .orElseThrow(() -> new IllegalStateException("Rol no encontrado para el usuario " + u.getCorreo()));
    }

    /** Variante segura que no lanza si no hay usuario o rol (útil en vistas). */
    public java.util.Optional<Rol> rolActualOptional() {
        return usuarioActual().flatMap(u -> rolRepo.findById(u.getIdRol()));
    }
}
