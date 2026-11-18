package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.Trazabilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrazabilidadRepository extends JpaRepository<Trazabilidad, Long> {
    java.util.List<Trazabilidad> findByIdMaterialOrderByIdTrazabilidadDesc(Long idMaterial);
}
