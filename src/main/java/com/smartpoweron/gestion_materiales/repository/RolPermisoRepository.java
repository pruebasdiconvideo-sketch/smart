package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.RolPermiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolPermisoRepository extends JpaRepository<RolPermiso, com.smartpoweron.gestion_materiales.entity.RolPermiso.RolPermisoId> {}
