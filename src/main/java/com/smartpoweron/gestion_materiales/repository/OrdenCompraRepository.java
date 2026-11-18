package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {
    java.util.List<OrdenCompra> findByEstado(String estado);
    java.util.Optional<OrdenCompra> findByCodigo(String codigo);
    boolean existsByCodigoAndIdOrdenCompraNot(String codigo, Long idOrdenCompra);
}
