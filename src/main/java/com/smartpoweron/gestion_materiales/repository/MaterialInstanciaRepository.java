package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.MaterialInstancia;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialInstanciaRepository extends JpaRepository<MaterialInstancia, Long> {
    Optional<MaterialInstancia> findByNumeroSerie(String numeroSerie);
    boolean existsByNumeroSerie(String numeroSerie);
    List<MaterialInstancia> findByIdMaterialAndEstadoOrderByIdInstanciaAsc(Long idMaterial, String estado, Pageable pageable);
    long countByIdMaterialAndEstado(Long idMaterial, String estado);
    List<MaterialInstancia> findByIdMaterialOrderByIdInstanciaDesc(Long idMaterial);
}