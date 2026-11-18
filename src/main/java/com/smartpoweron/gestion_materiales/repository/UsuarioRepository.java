package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    Optional<Usuario> findByCorreoAndActivoTrue(String correo);
    boolean existsByCorreo(String correo);
    boolean existsByCorreoAndIdUsuarioNot(String correo, Long idUsuario);
}