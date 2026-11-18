package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.DetalleOrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleOrdenCompraRepository extends JpaRepository<DetalleOrdenCompra, Long> {
    List<DetalleOrdenCompra> findByIdOrdenCompra(Long idOrdenCompra);
    void deleteByIdOrdenCompra(Long idOrdenCompra);
}
