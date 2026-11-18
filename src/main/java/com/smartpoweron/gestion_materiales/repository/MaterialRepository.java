package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    Optional<Material> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    boolean existsByCodigoAndIdMaterialNot(String codigo, Long idMaterial);
}