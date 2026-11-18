package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Long> {
}