package com.smartpoweron.gestion_materiales.service;

import com.smartpoweron.gestion_materiales.dto.IngresoMaterialDTO;
import com.smartpoweron.gestion_materiales.dto.ReposicionMaterialDTO;
import com.smartpoweron.gestion_materiales.entity.*;
import com.smartpoweron.gestion_materiales.exception.ReglaNegocioException;
import com.smartpoweron.gestion_materiales.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventarioService {

    @Autowired private OrdenCompraRepository ordenCompraRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private MovimientoInventarioRepository movimientoRepository;
    @Autowired private ProyectoRepository proyectoRepository;

    @Transactional
    public void registrarIngreso(IngresoMaterialDTO dto) {
        // 1. Validar Orden de Compra
        OrdenCompra orden = ordenCompraRepository.findById(dto.getOrdenCompraId())
                .orElseThrow(() -> new ReglaNegocioException("La Orden de Compra no existe."));

        if (!"ABIERTA".equals(orden.getEstado())) {
            throw new ReglaNegocioException("Solo se puede ingresar material contra una OC ABIERTA.");
        }

        // 2. Validar Material
        Material material = materialRepository.findByCodigo(dto.getCodigoMaterial())
                .orElseThrow(() -> new ReglaNegocioException("El material no existe en el catálogo."));

        // 3. Actualizar Stock
        material.setStockActual(material.getStockActual() + dto.getCantidad());
        materialRepository.save(material);

        // 4. Registrar Movimiento
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setMaterial(material);
        movimiento.setTipoMovimiento("INGRESO");
        movimiento.setCantidad(dto.getCantidad());
        movimiento.setOrdenCompra(orden);
        movimientoRepository.save(movimiento);
    }

    @Transactional
    public void registrarReposicion(ReposicionMaterialDTO dto) {
        // 1. Validar Proyecto
        Proyecto proyecto = proyectoRepository.findById(dto.getProyectoId())
                .orElseThrow(() -> new ReglaNegocioException("El proyecto no existe."));

        // 2. Validar Material
        Material material = materialRepository.findByCodigo(dto.getCodigoMaterial())
                .orElseThrow(() -> new ReglaNegocioException("El material no existe."));

        // 3. Actualizar Stock
        material.setStockActual(material.getStockActual() + dto.getCantidadSobrante());
        materialRepository.save(material);

        // 4. Registrar Movimiento
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setMaterial(material);
        movimiento.setTipoMovimiento("REPOSICION");
        movimiento.setCantidad(dto.getCantidadSobrante());
        movimiento.setProyecto(proyecto);
        movimientoRepository.save(movimiento);
    }
}