package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.Despacho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DespachoRepository extends JpaRepository<Despacho, Long> {
    java.util.List<Despacho> findByIdSolicitud(Long idSolicitud);
    java.util.List<Despacho> findByEstado(String estado);
}
