package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntregaRepository extends JpaRepository<Entrega, Long> {
    boolean existsByIdDespacho(Long idDespacho);
    java.util.List<Entrega> findByIdDespacho(Long idDespacho);
}
