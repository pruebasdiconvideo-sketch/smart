package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.DetalleDespacho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleDespachoRepository extends JpaRepository<DetalleDespacho, Long> {
    java.util.List<DetalleDespacho> findByIdDespacho(Long idDespacho);
}
