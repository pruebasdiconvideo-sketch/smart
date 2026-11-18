package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.MaterialInstancia;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialInstanciaRepository extends JpaRepository<MaterialInstancia, Long> {

    /** Lookup de una instancia por su número de serie (trazabilidad individual). */
    Optional<MaterialInstancia> findByNumeroSerie(String numeroSerie);

    /** Validación de unicidad del número de serie al ingresar. */
    boolean existsByNumeroSerie(String numeroSerie);

    /** Instancias EN_ALMACEN de un material, ordenadas por fecha de ingreso (FIFO) para despacho/merma. */
    List<MaterialInstancia> findByIdMaterialAndEstadoOrderByIdInstanciaAsc(Long idMaterial, String estado, Pageable pageable);

    /** Conteo de instancias en almacén por material. */
    long countByIdMaterialAndEstado(Long idMaterial, String estado);

    /** Listado de instancias de un material. */
    List<MaterialInstancia> findByIdMaterialOrderByIdInstanciaDesc(Long idMaterial);
}
