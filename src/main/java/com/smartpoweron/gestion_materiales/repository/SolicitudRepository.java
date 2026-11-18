package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    java.util.List<Solicitud> findByEstado(String estado);
    java.util.List<Solicitud> findByEstadoIn(java.util.List<String> estados);
}
