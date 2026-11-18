package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    boolean existsByCodigo(String codigo);
    boolean existsByCodigoAndIdProyectoNot(String codigo, Long idProyecto);
}