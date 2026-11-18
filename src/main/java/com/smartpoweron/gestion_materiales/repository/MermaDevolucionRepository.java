package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.MermaDevolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MermaDevolucionRepository extends JpaRepository<MermaDevolucion, Long> {
    java.util.List<MermaDevolucion> findByIdProyecto(Long idProyecto);
    java.util.List<MermaDevolucion> findByTipoAndEstado(String tipo, String estado);
    java.util.List<MermaDevolucion> findByEstado(String estado);
}
