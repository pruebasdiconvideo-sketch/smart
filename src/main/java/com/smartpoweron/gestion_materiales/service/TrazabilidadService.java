package com.smartpoweron.gestion_materiales.service;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.entity.Material;
import com.smartpoweron.gestion_materiales.entity.OrdenCompra;
import com.smartpoweron.gestion_materiales.entity.Proyecto;
import com.smartpoweron.gestion_materiales.entity.Trazabilidad;
import com.smartpoweron.gestion_materiales.entity.Usuario;
import com.smartpoweron.gestion_materiales.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.List;

/**
 * Servicio interno de trazabilidad (§7.2 - M6).
 *
 * - {@link #registrarMovimiento(Long, Long, Long, Long, String, Integer)} se
 *   invoca DENTRO de la transacción de cada cambio de stock (R-INV-02 / RNF06).
 *   Es sólo inserción: nunca expone UPDATE ni DELETE (RNF07 / P5-RN03).
 * - {@link #consultarPorMaterial(String)} (RF36) es el único servicio API.
 */
@Service
public class TrazabilidadService {

    private final TrazabilidadRepository trazRepo;
    private final MaterialRepository materialRepo;
    private final UsuarioRepository usuarioRepo;
    private final ProyectoRepository proyectoRepo;
    private final OrdenCompraRepository ocRepo;

    public TrazabilidadService(TrazabilidadRepository trazRepo,
                               MaterialRepository materialRepo,
                               UsuarioRepository usuarioRepo,
                               ProyectoRepository proyectoRepo,
                               OrdenCompraRepository ocRepo) {
        this.trazRepo = trazRepo;
        this.materialRepo = materialRepo;
        this.usuarioRepo = usuarioRepo;
        this.proyectoRepo = proyectoRepo;
        this.ocRepo = ocRepo;
    }

    /**
     * Inserta un movimiento de trazabilidad en la MISMA transacción del cambio
     * de stock (R-INV-02). Propagation.MANDATORY exige un contexto transaccional
     * externo: si alguien la invoca fuera de una transacción, falla y no se
     * queda un movimiento huérfano.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarMovimiento(Long idMaterial, Long idUsuario, Long idProyecto,
                                    Long idOrdenCompra, String tipoMovimiento, Integer cantidad) {
        Trazabilidad t = Trazabilidad.builder()
                .idMaterial(idMaterial)
                .idUsuario(idUsuario)
                .idProyecto(idProyecto)
                .idOrdenCompra(idOrdenCompra)
                .tipoMovimiento(tipoMovimiento)
                .cantidad(cantidad)
                .fechaMovimiento(LocalDateTime.now())
                .build();
        trazRepo.save(t);   // sólo INSERT (RNF07)
    }

    /** RF36: historial de un material por su código de fabricante. */
    @Transactional(readOnly = true)
    public List<Dtos.TrazabilidadDto> consultarPorMaterial(String codigo) {
        Material m = materialRepo.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("El material con código " + codigo + " no existe."));
        List<Trazabilidad> movs = trazRepo.findByIdMaterialOrderByIdTrazabilidadDesc(m.getIdMaterial());
        return movs.stream().map(t -> {
            Usuario u = usuarioRepo.findById(t.getIdUsuario()).orElse(null);
            Proyecto p = t.getIdProyecto() == null ? null : proyectoRepo.findById(t.getIdProyecto()).orElse(null);
            OrdenCompra oc = t.getIdOrdenCompra() == null ? null : ocRepo.findById(t.getIdOrdenCompra()).orElse(null);
            return Dtos.TrazabilidadDto.builder()
                    .idTrazabilidad(t.getIdTrazabilidad())
                    .codigoMaterial(m.getCodigo())
                    .nombreMaterial(m.getNombre())
                    .tipoMovimiento(t.getTipoMovimiento())
                    .cantidad(t.getCantidad())
                    .usuario(u == null ? null : u.getNombreCompleto())
                    .codigoProyecto(p == null ? null : p.getCodigo())
                    .codigoOrdenCompra(oc == null ? null : oc.getCodigo())
                    .fechaMovimiento(t.getFechaMovimiento())
                    .build();
        }).toList();
    }
}
