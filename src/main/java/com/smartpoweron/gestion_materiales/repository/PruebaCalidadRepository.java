package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.PruebaCalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PruebaCalidadRepository extends JpaRepository<PruebaCalidad, Long> {
    java.util.List<PruebaCalidad> findByIdProyecto(Long idProyecto);
    java.util.List<PruebaCalidad> findByIdProyectoOrderByIdPruebaDesc(Long idProyecto);
}
