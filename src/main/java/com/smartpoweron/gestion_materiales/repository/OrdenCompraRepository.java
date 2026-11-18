package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {
    Optional<OrdenCompra> findByCodigo(String codigo);
    List<OrdenCompra> findByEstado(String estado); // <-- Faltaba
    boolean existsByCodigoAndIdOrdenCompraNot(String codigo, Long id); // <-- Faltaba
}