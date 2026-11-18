package com.smartpoweron.gestion_materiales.security;

import com.smartpoweron.gestion_materiales.entity.Rol;
import com.smartpoweron.gestion_materiales.entity.Usuario;
import com.smartpoweron.gestion_materiales.repository.RolRepository;
import com.smartpoweron.gestion_materiales.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Carga al usuario desde la tabla `usuario` y resuelve su rol desde `rol`
 * (RF01, RF03). Las contraseñas se almacenan con BCrypt (RNF04) y la autoridad
 * se publica como ROLE_<nombre_rol> (Spring Security exige el prefijo ROLE_).
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepo;
    private final RolRepository rolRepo;

    public UsuarioDetailsService(UsuarioRepository usuarioRepo, RolRepository rolRepo) {
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) {
        Optional<Usuario> opt = usuarioRepo.findByCorreo(correo);
        if (opt.isEmpty()) {
            throw new UsernameNotFoundException("Usuario o contraseña incorrectos");
        }
        Usuario u = opt.get();
        if (!Boolean.TRUE.equals(u.getActivo())) {
            throw new UsernameNotFoundException("El usuario está inactivo");
        }
        Rol rol = rolRepo.findById(u.getIdRol())
                .orElseThrow(() -> new UsernameNotFoundException("El rol asignado no existe"));
        GrantedAuthority auth = new SimpleGrantedAuthority("ROLE_" + rol.getNombre());
        return User.withUsername(u.getCorreo())
                .password(u.getPasswordHash())
                .authorities(List.of(auth))
                .accountLocked(false)
                .disabled(false)
                .build();
    }

    /** Devuelve la entidad Usuario para que el AuthService pueda firmar el JWT. */
    public Usuario obtenerEntidadPorCorreo(String correo) {
        return usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario o contraseña incorrectos"));
    }
}
