package com.smartpoweron.gestion_materiales.service;

import com.smartpoweron.gestion_materiales.model.SolicitudProduccion;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProduccionService {

    // Implementación básica (puedes ampliarla según tus necesidades)
    public List<SolicitudProduccion> listarSolicitudes() {
        return List.of(); // Retorna una lista vacía por ahora
    }

    public SolicitudProduccion crearSolicitud(SolicitudProduccion solicitud) {
        return solicitud; // Lógica simple, ajusta según corresponda
    }
}