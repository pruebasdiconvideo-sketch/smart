package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    List<Empresa> findByTipo(String tipo);
    List<Empresa> findByTipoAndEstado(String tipo, String estado);
    Optional<Empresa> findByRuc(String ruc);
    boolean existsByRuc(String ruc);
    boolean existsByRucAndIdEmpresaNot(String ruc, Long idEmpresa);
}