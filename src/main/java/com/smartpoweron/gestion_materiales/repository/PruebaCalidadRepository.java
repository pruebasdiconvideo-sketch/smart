package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.PruebaCalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PruebaCalidadRepository extends JpaRepository<PruebaCalidad, Long> {
    List<PruebaCalidad> findByIdProyecto(Long idProyecto);
    // Método añadido (error CalidadService:128,207)
    List<PruebaCalidad> findByIdProyectoOrderByIdPruebaDesc(Long idProyecto); 
}