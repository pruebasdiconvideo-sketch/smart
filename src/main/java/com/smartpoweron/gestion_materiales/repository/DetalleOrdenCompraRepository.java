package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.DetalleOrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DetalleOrdenCompraRepository extends JpaRepository<DetalleOrdenCompra, Long> {
    List<DetalleOrdenCompra> findByIdOrdenCompra(Long idOrdenCompra);
}