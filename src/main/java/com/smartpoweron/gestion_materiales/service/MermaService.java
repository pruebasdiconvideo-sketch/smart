package com.smartpoweron.gestion_materiales.service;
import com.smartpoweron.gestion_materiales.repository.ProyectoRepository;
import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.entity.*;
import com.smartpoweron.gestion_materiales.exception.ReglaNegocioException;
import com.smartpoweron.gestion_materiales.repository.*;
import com.smartpoweron.gestion_materiales.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de mermas y devoluciones (RF31 a RF35, §7.2 - M5).
 *
 * Reglas aplicadas:
 *   P5-RN01 / R-INV-01 / RF32: la DEVOLUCIÓN en buen estado → REINTEGRADO,
 *       suma stock y registra movimiento DEVOLUCION (en la misma transacción).
 *   P5-RN02 / R-INV-01 / RF34: la MERMA sólo pasa a MERMADO con la validación
 *       del JEFE_PRODUCCION; NUNCA aumenta el stock; registra movimiento MERMA.
 *   R-INV-02: el cambio de stock y el movimiento de trazabilidad van juntos.
 */
@Service
public class MermaService {

    private final MermaDevolucionRepository mermaRepo;
    private final MaterialRepository materialRepo;
    private final ProyectoRepository proyectoRepo;
    private final UsuarioRepository usuarioRepo;
    private final TrazabilidadService trazabilidadService;
    private final SecurityUtil securityUtil;
    private final MaterialService materialService;

    public MermaService(MermaDevolucionRepository mermaRepo,
                        MaterialRepository materialRepo,
                        ProyectoRepository proyectoRepo,
                        UsuarioRepository usuarioRepo,
                        TrazabilidadService trazabilidadService,
                        SecurityUtil securityUtil,
                        MaterialService materialService) {
        this.mermaRepo = mermaRepo;
        this.materialRepo = materialRepo;
        this.proyectoRepo = proyectoRepo;
        this.usuarioRepo = usuarioRepo;
        this.trazabilidadService = trazabilidadService;
        this.securityUtil = securityUtil;
        this.materialService = materialService;
    }

    // ===== Consulta de devoluciones (auxiliar de la vista V12) =====
    @Transactional(readOnly = true)
    public List<Dtos.MermaDevolucionDto> listarTodas() {
        return mermaRepo.findAll().stream().map(this::toDto).toList();
    }

    // ===== RF31 - operario registra devolución de sobrantes =====
    @Transactional
    public Dtos.MermaDevolucionDto registrarDevolucion(Dtos.DevolucionCreate req) {
        Usuario operario = securityUtil.usuarioOExcepcion();
        Proyecto proyecto = proyectoRepo.findById(req.getIdProyecto()).orElseThrow(() ->
                new ReglaNegocioException("El proyecto no existe."));
        if ("ANULADO".equals(proyecto.getEstado())) {
            throw new ReglaNegocioException("No se pueden registrar devoluciones para un proyecto anulado.");
        }
        Material m = materialRepo.findById(req.getIdMaterial()).orElseThrow(() ->
                new ReglaNegocioException("El material no existe."));
        if (!"ACTIVO".equals(m.getEstado())) {
            throw new ReglaNegocioException("No se pueden registrar devoluciones para un material inactivo.");
        }
        if (req.getCantidad() == null || req.getCantidad() <= 0) {
            throw new ReglaNegocioException("La cantidad debe ser mayor que cero.");
        }
        MermaDevolucion md = MermaDevolucion.builder()
                .idProyecto(req.getIdProyecto())
                .idMaterial(m.getIdMaterial())
                .idUsuarioRegistra(operario.getIdUsuario())
                .tipo("DEVOLUCION")
                .cantidad(req.getCantidad())
                .estado("PENDIENTE")
                .fecha(LocalDateTime.now())
                .build();
        return toDto(mermaRepo.save(md));
    }

    @Transactional
    public Dtos.MermaDevolucionDto actualizar(Long id, Dtos.DevolucionCreate req) {
        MermaDevolucion md = mermaRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La devolución no existe."));
        if (Boolean.TRUE.equals(md.getAnulada()) || !"PENDIENTE".equals(md.getEstado())) {
            throw new ReglaNegocioException("Sólo se pueden editar registros pendientes y activos.");
        }
        Proyecto proyecto = proyectoRepo.findById(req.getIdProyecto()).orElseThrow(() ->
                new ReglaNegocioException("El proyecto no existe."));
        Material material = materialRepo.findById(req.getIdMaterial()).orElseThrow(() ->
                new ReglaNegocioException("El material no existe."));
        if ("ANULADO".equals(proyecto.getEstado()) || !"ACTIVO".equals(material.getEstado())) {
            throw new ReglaNegocioException("El proyecto y el material deben estar activos.");
        }
        if (req.getCantidad() == null || req.getCantidad() <= 0) {
            throw new ReglaNegocioException("La cantidad debe ser mayor que cero.");
        }
        md.setIdProyecto(proyecto.getIdProyecto());
        md.setIdMaterial(material.getIdMaterial());
        md.setCantidad(req.getCantidad());
        return toDto(mermaRepo.save(md));
    }

    @Transactional
    public void anular(Long id, String motivo) {
        Usuario actor = securityUtil.usuarioOExcepcion();
        MermaDevolucion md = mermaRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("El registro de devolución/merma no existe."));
        if (Boolean.TRUE.equals(md.getAnulada())) {
            throw new ReglaNegocioException("El registro ya está anulado.");
        }
        if ("REINTEGRADO".equals(md.getEstado())) {
            Material material = materialRepo.findById(md.getIdMaterial()).orElseThrow(() ->
                    new ReglaNegocioException("El material asociado no existe."));
            if (material.getStockActual() < md.getCantidad()) {
                throw new ReglaNegocioException("No hay stock suficiente para compensar el reintegro.");
            }
            material.setStockActual(material.getStockActual() - md.getCantidad());
            materialRepo.save(material);
            // Trazabilidad individual: marcar instancias como MERMADO (FIFO best-effort)
            materialService.marcarMermaInstancias(material.getIdMaterial(), md.getCantidad());
            trazabilidadService.registrarMovimiento(material.getIdMaterial(), actor.getIdUsuario(),
                    md.getIdProyecto(), null, "MERMA", md.getCantidad());
        } else if ("MERMADO".equals(md.getEstado())) {
            Material material = materialRepo.findById(md.getIdMaterial()).orElseThrow(() ->
                    new ReglaNegocioException("El material asociado no existe."));
            material.setStockActual(material.getStockActual() + md.getCantidad());
            materialRepo.save(material);
            trazabilidadService.registrarMovimiento(material.getIdMaterial(), actor.getIdUsuario(),
                    md.getIdProyecto(), null, "DEVOLUCION", md.getCantidad());
        }
        md.setAnulada(true);
        md.setMotivoAnulacion(motivo.trim());
        mermaRepo.save(md);
    }

    // ===== RF32 - almacenero reintegra la devolución =====
    @Transactional
    public Dtos.MermaDevolucionDto reintegrar(Long id) {
        Usuario almacenero = securityUtil.usuarioOExcepcion();
        MermaDevolucion md = mermaRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La devolución no existe."));
        if (!"DEVOLUCION".equals(md.getTipo())) {
            throw new ReglaNegocioException("Sólo se reintegran DEVOLUCIONES.");
        }
        if (!"PENDIENTE".equals(md.getEstado())) {
            throw new ReglaNegocioException("La devolución ya fue procesada.");
        }
        if (Boolean.TRUE.equals(md.getAnulada())) {
            throw new ReglaNegocioException("La devolución está anulada.");
        }
        Material m = materialRepo.findById(md.getIdMaterial()).orElseThrow();
        // R-INV-01 + R-INV-02: sumar stock y trazabilidad DEVOLUCION juntos
        m.setStockActual(m.getStockActual() + md.getCantidad());
        materialRepo.save(m);
        trazabilidadService.registrarMovimiento(
                m.getIdMaterial(), almacenero.getIdUsuario(), md.getIdProyecto(), null,
                "DEVOLUCION", md.getCantidad());
        md.setEstado("REINTEGRADO");
        md.setIdUsuarioValidador(almacenero.getIdUsuario());
        return toDto(mermaRepo.save(md));
    }

    // ===== RF33 - almacenero marca como merma =====
    @Transactional
    public Dtos.MermaDevolucionDto marcarMerma(Long id, Dtos.MarcarMermaRequest req) {
        Usuario almacenero = securityUtil.usuarioOExcepcion();
        MermaDevolucion md = mermaRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La devolución no existe."));
        if (!"PENDIENTE".equals(md.getEstado())) {
            throw new ReglaNegocioException("La devolución ya fue procesada.");
        }
        if (Boolean.TRUE.equals(md.getAnulada())) {
            throw new ReglaNegocioException("La devolución está anulada.");
        }
        md.setTipo("MERMA");
        md.setMotivo(req.getMotivo());
        md.setEstado("PENDIENTE");  // queda pendiente de validación del Jefe
        md.setIdUsuarioValidador(almacenero.getIdUsuario()); // el almacenero marca
        return toDto(mermaRepo.save(md));
    }

    // ===== RF34 - jefe de producción valida la baja por merma =====
    @Transactional
    public Dtos.MermaDevolucionDto validarBaja(Long id) {
        Usuario jefe = securityUtil.usuarioOExcepcion();
        MermaDevolucion md = mermaRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La merma no existe."));
        if (!"MERMA".equals(md.getTipo())) {
            throw new ReglaNegocioException("Sólo se validan MERMAS.");
        }
        if (!"PENDIENTE".equals(md.getEstado())) {
            throw new ReglaNegocioException("La merma ya fue procesada.");
        }
        if (Boolean.TRUE.equals(md.getAnulada())) {
            throw new ReglaNegocioException("La merma está anulada.");
        }
        Material m = materialRepo.findById(md.getIdMaterial()).orElseThrow();
        // R-INV-01: la merma validada NUNCA aumenta el stock. Sólo trazabilidad.
        trazabilidadService.registrarMovimiento(
                m.getIdMaterial(), jefe.getIdUsuario(), md.getIdProyecto(), null,
                "MERMA", md.getCantidad());
        md.setEstado("MERMADO");
        md.setIdUsuarioValidador(jefe.getIdUsuario());
        return toDto(mermaRepo.save(md));
    }

    // ===== RF35 - reporte de mermas por proyecto y fechas =====
    @Transactional(readOnly = true)
    public List<Dtos.ReporteMermaItem> reporteMermas(Long idProyecto, String desde, String hasta) {
        List<MermaDevolucion> mermas = mermaRepo.findByTipoAndEstado("MERMA", "MERMADO").stream()
                .filter(md -> !Boolean.TRUE.equals(md.getAnulada()))
                .toList();
        List<Dtos.ReporteMermaItem> out = new ArrayList<>();
        for (MermaDevolucion md : mermas) {
            if (idProyecto != null && !idProyecto.equals(md.getIdProyecto())) continue;
            if (desde != null && !desde.isBlank() && md.getFecha() != null
                    && md.getFecha().toLocalDate().toString().compareTo(desde) < 0) continue;
            if (hasta != null && !hasta.isBlank() && md.getFecha() != null
                    && md.getFecha().toLocalDate().toString().compareTo(hasta) > 0) continue;
            Proyecto p = proyectoRepo.findById(md.getIdProyecto()).orElse(null);
            Material m = materialRepo.findById(md.getIdMaterial()).orElse(null);
            BigDecimal costo = m == null ? BigDecimal.ZERO
                    : m.getPrecioUnitario().multiply(BigDecimal.valueOf(md.getCantidad()));
            out.add(Dtos.ReporteMermaItem.builder()
                    .codigoProyecto(p == null ? null : p.getCodigo())
                    .codigoMaterial(m == null ? null : m.getCodigo())
                    .nombreMaterial(m == null ? null : m.getNombre())
                    .motivo(md.getMotivo())
                    .cantidad(md.getCantidad())
                    .costoEstimado(costo)
                    .build());
        }
        return out;
    }

    // ===== Mapper =====
    @Transactional(readOnly = true)
    public Dtos.MermaDevolucionDto toDto(MermaDevolucion md) {
        Proyecto p = proyectoRepo.findById(md.getIdProyecto()).orElse(null);
        Material m = materialRepo.findById(md.getIdMaterial()).orElse(null);
        Usuario registra = usuarioRepo.findById(md.getIdUsuarioRegistra()).orElse(null);
        Usuario valida = md.getIdUsuarioValidador() == null ? null
                : usuarioRepo.findById(md.getIdUsuarioValidador()).orElse(null);
        return Dtos.MermaDevolucionDto.builder()
                .idMerma(md.getIdMerma())
                .idProyecto(md.getIdProyecto())
                .codigoProyecto(p == null ? null : p.getCodigo())
                .idMaterial(md.getIdMaterial())
                .codigoMaterial(m == null ? null : m.getCodigo())
                .nombreMaterial(m == null ? null : m.getNombre())
                .tipo(md.getTipo())
                .cantidad(md.getCantidad())
                .motivo(md.getMotivo())
                .estado(md.getEstado())
                .anulada(md.getAnulada())
                .motivoAnulacion(md.getMotivoAnulacion())
                .registra(registra == null ? null : registra.getNombreCompleto())
                .validador(valida == null ? null : valida.getNombreCompleto())
                .fecha(md.getFecha())
                .build();
    }
}
