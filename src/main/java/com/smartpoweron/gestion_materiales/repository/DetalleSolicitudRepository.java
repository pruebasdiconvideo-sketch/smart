package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.DetalleSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleSolicitudRepository extends JpaRepository<DetalleSolicitud, Long> {
    java.util.List<DetalleSolicitud> findByIdSolicitud(Long idSolicitud);
}
