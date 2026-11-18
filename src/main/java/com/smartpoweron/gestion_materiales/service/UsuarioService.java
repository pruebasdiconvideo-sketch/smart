package com.smartpoweron.gestion_materiales.service;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.entity.Rol;
import com.smartpoweron.gestion_materiales.entity.Usuario;
import com.smartpoweron.gestion_materiales.exception.ReglaNegocioException;
import com.smartpoweron.gestion_materiales.repository.RolRepository;
import com.smartpoweron.gestion_materiales.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestión de usuarios y roles (RF03, §7.2 - M0).
 * - crear: valida correo único y guarda la contraseña con BCrypt (RNF04).
 * - actualizar: cambia rol y nombre; nunca el password desde aquí.
 * - desactivar: marca activo=false; el usuario no podrá iniciar sesión (A0-T02).
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final RolRepository rolRepo;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarioRepo, RolRepository rolRepo, PasswordEncoder encoder) {
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<Dtos.UsuarioDto> listar() {
        return usuarioRepo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public Dtos.UsuarioDto crear(Dtos.UsuarioCreate req) {
        rolRepo.findById(req.getIdRol()).orElseThrow(() ->
                new ReglaNegocioException("El rol seleccionado no existe."));
        if (usuarioRepo.existsByCorreo(req.getCorreo())) {
            throw new ReglaNegocioException("El correo ya está registrado.");
        }
        Usuario u = Usuario.builder()
                .idRol(req.getIdRol())
                .nombreCompleto(req.getNombreCompleto())
                .correo(req.getCorreo())
                .passwordHash(encoder.encode(req.getPassword()))
                .activo(true)
                .build();
        return toDto(usuarioRepo.save(u));
    }

    @Transactional
    public Dtos.UsuarioDto actualizar(Long id, Dtos.UsuarioUpdate req) {
        Usuario u = usuarioRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("El usuario no existe."));
        if (!Boolean.TRUE.equals(u.getActivo())) {
            throw new ReglaNegocioException("No se puede editar un usuario inactivo.");
        }
        rolRepo.findById(req.getIdRol()).orElseThrow(() ->
                new ReglaNegocioException("El rol seleccionado no existe."));
        if (usuarioRepo.existsByCorreoAndIdUsuarioNot(req.getCorreo(), id)) {
            throw new ReglaNegocioException("El correo ya está registrado.");
        }
        u.setIdRol(req.getIdRol());
        u.setNombreCompleto(req.getNombreCompleto());
        u.setCorreo(req.getCorreo());
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            u.setPasswordHash(encoder.encode(req.getPassword()));
        }
        return toDto(usuarioRepo.save(u));
    }

    @Transactional
    public void desactivar(Long id) {
        Usuario u = usuarioRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("El usuario no existe."));
        if (!Boolean.TRUE.equals(u.getActivo())) {
            throw new ReglaNegocioException("El usuario ya está inactivo.");
        }
        u.setActivo(false);
        usuarioRepo.save(u);
    }

    @Transactional
    public void reactivar(Long id) {
        Usuario u = usuarioRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("El usuario no existe."));
        if (Boolean.TRUE.equals(u.getActivo())) {
            throw new ReglaNegocioException("El usuario ya está activo.");
        }
        u.setActivo(true);
        usuarioRepo.save(u);
    }

    private Dtos.UsuarioDto toDto(Usuario u) {
        Rol r = rolRepo.findById(u.getIdRol()).orElse(null);
        return Dtos.UsuarioDto.builder()
                .idUsuario(u.getIdUsuario())
                .idRol(u.getIdRol())
                .rol(r == null ? null : r.getNombre())
                .nombreCompleto(u.getNombreCompleto())
                .correo(u.getCorreo())
                .activo(u.getActivo())
                .ultimoAcceso(u.getUltimoAcceso())
                .build();
    }
}
