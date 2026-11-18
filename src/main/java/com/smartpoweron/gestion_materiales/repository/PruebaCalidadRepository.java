package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.PruebaCalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PruebaCalidadRepository extends JpaRepository<PruebaCalidad, Long> {
    List<PruebaCalidad> findByIdProyecto(Long idProyecto);
}