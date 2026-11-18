package com.smartpoweron.gestion_materiales.repository;

import com.smartpoweron.gestion_materiales.entity.ParametroPrueba;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametroPruebaRepository extends JpaRepository<ParametroPrueba, Long> {}
