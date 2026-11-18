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
 * Servicio de solicitudes con BOM y su ciclo de estados (RF13 a RF18, §7.2 - M2).
 *
 * Reglas aplicadas:
 *   P2-RN01 / RF14: cada componente debe existir en el catálogo y cantidad > 0.
 *   R-INV-06 / RF15: el costo total = Σ cantidad × precio unitario; si supera el
 *                    presupuesto del proyecto → estado RETENIDA.
 *   RF16: sólo el ADMIN aprueba o rechaza (lo refuerza el SecurityConfig).
 *   P2-RN03 / RF17: sólo pasa a LISTA_DESPACHO si todas las líneas tienen stock.
 *   RF18: si falta stock, se genera el listado de faltantes.
 */
@Service
public class SolicitudService {

    private final SolicitudRepository solicitudRepo;
    private final DetalleSolicitudRepository detalleRepo;
    private final MaterialRepository materialRepo;
    private final ProyectoRepository proyectoRepo;
    private final UsuarioRepository usuarioRepo;
    private final DespachoRepository despachoRepo;
    private final DetalleDespachoRepository detalleDespachoRepo;
    private final SecurityUtil securityUtil;
    private final TrazabilidadService trazabilidadService;

    public SolicitudService(SolicitudRepository solicitudRepo,
                            DetalleSolicitudRepository detalleRepo,
                            MaterialRepository materialRepo,
                            ProyectoRepository proyectoRepo,
                            UsuarioRepository usuarioRepo,
                            DespachoRepository despachoRepo,
                            DetalleDespachoRepository detalleDespachoRepo,
                            SecurityUtil securityUtil,
                            TrazabilidadService trazabilidadService) {
        this.solicitudRepo = solicitudRepo;
        this.detalleRepo = detalleRepo;
        this.materialRepo = materialRepo;
        this.proyectoRepo = proyectoRepo;
        this.usuarioRepo = usuarioRepo;
        this.despachoRepo = despachoRepo;
        this.detalleDespachoRepo = detalleDespachoRepo;
        this.securityUtil = securityUtil;
        this.trazabilidadService = trazabilidadService;
    }

    // ===== RF13, RF14, RF15 - registrar solicitud con BOM =====
    @Transactional
    public Dtos.SolicitudDto registrarConBom(Dtos.SolicitudCreate req) {
        Usuario solicitante = securityUtil.usuarioOExcepcion();
        Proyecto p = proyectoRepo.findById(req.getIdProyecto()).orElseThrow(() ->
                new ReglaNegocioException("El proyecto no existe."));
        if ("ANULADO".equals(p.getEstado())) {
            throw new ReglaNegocioException("No se pueden registrar solicitudes para un proyecto anulado.");
        }
        if (req.getBom() == null || req.getBom().isEmpty()) {
            throw new ReglaNegocioException("La solicitud debe contener al menos un material.");
        }

        // P2-RN01: validar componentes y cantidades antes de guardar nada
        List<String> errores = new ArrayList<>();
        BigDecimal costoTotal = BigDecimal.ZERO;
        List<DetalleSolicitud> lineas = new ArrayList<>();
        for (int i = 0; i < req.getBom().size(); i++) {
            Dtos.DetalleSolicitudCreate line = req.getBom().get(i);
            if (line.getCantidad() == null || line.getCantidad() <= 0) {
                errores.add("Línea " + (i + 1) + ": la cantidad debe ser mayor que cero.");
                continue;
            }
            Material m = materialRepo.findById(line.getIdMaterial()).orElse(null);
            if (m == null) {
                errores.add("Línea " + (i + 1) + ": el material no existe en el catálogo.");
                continue;
            }
            if (!"ACTIVO".equals(m.getEstado())) {
                errores.add("Línea " + (i + 1) + ": el material está inactivo.");
                continue;
            }
            costoTotal = costoTotal.add(m.getPrecioUnitario().multiply(BigDecimal.valueOf(line.getCantidad())));
            DetalleSolicitud ds = DetalleSolicitud.builder()
                    .idMaterial(m.getIdMaterial())
                    .cantidadSolicitada(line.getCantidad())
                    .cantidadDespachada(0)
                    .build();
            lineas.add(ds);
        }
        if (!errores.isEmpty()) {
            // RF14: la solicitud no se acepta; se listan los errores por línea.
            throw new ReglaNegocioException("La solicitud tiene errores: " + String.join(" | ", errores));
        }

        // R-INV-06 / RF15: decidir estado según presupuesto
        String estado;
        if (costoTotal.compareTo(p.getPresupuesto()) > 0) {
            estado = "RETENIDA";
        } else {
            estado = "BORRADOR";
        }
        Solicitud s = Solicitud.builder()
                .idProyecto(p.getIdProyecto())
                .idUsuarioSolicitante(solicitante.getIdUsuario())
                .fechaSolicitud(LocalDateTime.now())
                .costoTotal(costoTotal)
                .estado(estado)
                .build();
        s = solicitudRepo.save(s);
        for (DetalleSolicitud ds : lineas) {
            ds.setIdSolicitud(s.getIdSolicitud());
            detalleRepo.save(ds);
        }
        // Si quedó BORRADOR, verificamos el stock inmediatamente (P2-T07/T08/T09)
        if ("BORRADOR".equals(estado)) {
            verificarStockInterno(s);
        }
        return toDto(s);
    }

    @Transactional
    public Dtos.SolicitudDto actualizar(Long id, Dtos.SolicitudCreate req) {
        Solicitud solicitud = solicitudRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La solicitud no existe."));
        if (Boolean.TRUE.equals(solicitud.getAnulada())) {
            throw new ReglaNegocioException("No se puede editar una solicitud anulada.");
        }
        if (!List.of("BORRADOR", "RETENIDA", "LISTA_DESPACHO", "RECHAZADA").contains(solicitud.getEstado())) {
            throw new ReglaNegocioException("No se puede editar una solicitud en estado " + solicitud.getEstado() + ".");
        }
        if (!despachoRepo.findByIdSolicitud(id).isEmpty()) {
            throw new ReglaNegocioException("No se puede editar una solicitud que ya tiene despachos registrados.");
        }
        if (req.getBom() == null || req.getBom().isEmpty()) {
            throw new ReglaNegocioException("La solicitud debe contener al menos un material.");
        }
        Proyecto proyecto = proyectoRepo.findById(req.getIdProyecto()).orElseThrow(() ->
                new ReglaNegocioException("El proyecto no existe."));
        if ("ANULADO".equals(proyecto.getEstado())) {
            throw new ReglaNegocioException("No se pueden registrar solicitudes para un proyecto anulado.");
        }

        BigDecimal costoTotal = BigDecimal.ZERO;
        List<DetalleSolicitud> lineas = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        for (int i = 0; i < req.getBom().size(); i++) {
            Dtos.DetalleSolicitudCreate linea = req.getBom().get(i);
            if (linea.getCantidad() == null || linea.getCantidad() <= 0) {
                errores.add("Línea " + (i + 1) + ": la cantidad debe ser mayor que cero.");
                continue;
            }
            Material material = materialRepo.findById(linea.getIdMaterial()).orElse(null);
            if (material == null || !"ACTIVO".equals(material.getEstado())) {
                errores.add("Línea " + (i + 1) + ": el material no existe o está inactivo.");
                continue;
            }
            costoTotal = costoTotal.add(material.getPrecioUnitario().multiply(BigDecimal.valueOf(linea.getCantidad())));
            lineas.add(DetalleSolicitud.builder()
                    .idMaterial(material.getIdMaterial())
                    .cantidadSolicitada(linea.getCantidad())
                    .cantidadDespachada(0)
                    .build());
        }
        if (!errores.isEmpty()) {
            throw new ReglaNegocioException("La solicitud tiene errores: " + String.join(" | ", errores));
        }

        detalleRepo.deleteAll(detalleRepo.findByIdSolicitud(id));
        solicitud.setIdProyecto(proyecto.getIdProyecto());
        solicitud.setCostoTotal(costoTotal);
        solicitud.setIdUsuarioAprobador(null);
        solicitud.setEstado(costoTotal.compareTo(proyecto.getPresupuesto()) > 0 ? "RETENIDA" : "BORRADOR");
        solicitud = solicitudRepo.save(solicitud);
        for (DetalleSolicitud linea : lineas) {
            linea.setIdSolicitud(solicitud.getIdSolicitud());
            detalleRepo.save(linea);
        }
        if ("BORRADOR".equals(solicitud.getEstado())) {
            verificarStockInterno(solicitud);
        }
        return toDto(solicitud);
    }

    @Transactional
    public Dtos.SolicitudDto anular(Long id, Dtos.SolicitudAnulacionRequest req) {
        Solicitud solicitud = solicitudRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La solicitud no existe."));
        if (Boolean.TRUE.equals(solicitud.getAnulada())) {
            throw new ReglaNegocioException("La solicitud ya está anulada.");
        }
        Usuario actor = securityUtil.usuarioOExcepcion();
        for (Despacho despacho : despachoRepo.findByIdSolicitud(id)) {
            if (Boolean.TRUE.equals(despacho.getAnulado())) {
                continue;
            }
            if ("CONFIRMADO".equals(despacho.getEstado())) {
                for (DetalleDespacho detalle : detalleDespachoRepo.findByIdDespacho(despacho.getIdDespacho())) {
                    Material material = materialRepo.findById(detalle.getIdMaterial()).orElseThrow(() ->
                            new ReglaNegocioException("No se encontró un material del despacho para compensarlo."));
                    DetalleSolicitud linea = detalleRepo.findById(detalle.getIdDetalleSolicitud()).orElseThrow(() ->
                            new ReglaNegocioException("No se encontró una línea de solicitud para compensarla."));
                    if (linea.getCantidadDespachada() < detalle.getCantidadEntregada()) {
                        throw new ReglaNegocioException("La cantidad despachada no coincide con el historial; no se puede anular.");
                    }
                    material.setStockActual(material.getStockActual() + detalle.getCantidadEntregada());
                    linea.setCantidadDespachada(linea.getCantidadDespachada() - detalle.getCantidadEntregada());
                    materialRepo.save(material);
                    detalleRepo.save(linea);
                    trazabilidadService.registrarMovimiento(material.getIdMaterial(), actor.getIdUsuario(),
                            solicitud.getIdProyecto(), null, "DEVOLUCION", detalle.getCantidadEntregada());
                }
            }
            despacho.setAnulado(true);
            despacho.setMotivoAnulacion(req.getMotivo().trim());
            despachoRepo.save(despacho);
        }
        solicitud.setAnulada(true);
        solicitud.setMotivoAnulacion(req.getMotivo().trim());
        solicitudRepo.save(solicitud);
        return toDto(solicitud);
    }

    // ===== RF15, RF16 - listar por estado =====
    @Transactional(readOnly = true)
    public List<Dtos.SolicitudDto> listarPorEstado(String estado) {
        List<Solicitud> lista = (estado == null || estado.isBlank())
                ? solicitudRepo.findAll()
                : solicitudRepo.findByEstado(estado);
        return lista.stream().map(this::toDto).toList();
    }

    // ===== RF16 - aprobar solicitud retenida =====
    @Transactional
    public Dtos.SolicitudDto aprobar(Long id, Dtos.AprobacionRequest req) {
        Solicitud s = solicitudRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La solicitud no existe."));
        if (Boolean.TRUE.equals(s.getAnulada())) {
            throw new ReglaNegocioException("La solicitud está anulada.");
        }
        if (!"RETENIDA".equals(s.getEstado())) {
            throw new ReglaNegocioException("Sólo se pueden aprobar solicitudes RETENIDAS.");
        }
        Usuario admin = securityUtil.usuarioOExcepcion();
        s.setIdUsuarioAprobador(admin.getIdUsuario());
        s.setEstado("BORRADOR");  // aprobada: continúa al verificar stock
        solicitudRepo.save(s);
        verificarStockInterno(s);
        return toDto(s);
    }

    // ===== RF16 - rechazar solicitud retenida =====
    @Transactional
    public Dtos.SolicitudDto rechazar(Long id, Dtos.AprobacionRequest req) {
        Solicitud s = solicitudRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La solicitud no existe."));
        if (Boolean.TRUE.equals(s.getAnulada())) {
            throw new ReglaNegocioException("La solicitud está anulada.");
        }
        if (!"RETENIDA".equals(s.getEstado())) {
            throw new ReglaNegocioException("Sólo se pueden rechazar solicitudes RETENIDAS.");
        }
        Usuario admin = securityUtil.usuarioOExcepcion();
        s.setIdUsuarioAprobador(admin.getIdUsuario());
        s.setEstado("RECHAZADA");
        solicitudRepo.save(s);
        return toDto(s);
    }

    // ===== RF17, RF18 - verificar stock y mover a LISTA_DESPACHO o listar faltantes =====
    @Transactional
    public Dtos.SolicitudDto verificarStock(Long id) {
        Solicitud s = solicitudRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La solicitud no existe."));
        if (Boolean.TRUE.equals(s.getAnulada())) {
            throw new ReglaNegocioException("La solicitud está anulada.");
        }
        if ("DESPACHADA".equals(s.getEstado()) || "RECHAZADA".equals(s.getEstado())) {
            throw new ReglaNegocioException("La solicitud ya está " + s.getEstado() + ".");
        }
        verificarStockInterno(s);
        return toDto(s);
    }

    @Transactional
    public void recalcularEstadoDespuesDeCompensacion(Long idSolicitud) {
        Solicitud solicitud = solicitudRepo.findById(idSolicitud).orElseThrow(() ->
                new ReglaNegocioException("La solicitud no existe."));
        if (!Boolean.TRUE.equals(solicitud.getAnulada())) {
            boolean completa = detalleRepo.findByIdSolicitud(idSolicitud).stream()
                    .allMatch(linea -> linea.getCantidadDespachada() >= linea.getCantidadSolicitada());
            if (completa) {
                solicitud.setEstado("DESPACHADA");
                solicitudRepo.save(solicitud);
            } else {
                verificarStockInterno(solicitud);
            }
        }
    }

    private void verificarStockInterno(Solicitud s) {
        List<DetalleSolicitud> lineas = detalleRepo.findByIdSolicitud(s.getIdSolicitud());
        boolean todoConStock = true;
        for (DetalleSolicitud ds : lineas) {
            Material m = materialRepo.findById(ds.getIdMaterial()).orElse(null);
            if (m == null) { todoConStock = false; break; }
            int saldo = ds.getCantidadSolicitada() - ds.getCantidadDespachada();
            if (saldo > 0 && m.getStockActual() < saldo) {
                todoConStock = false;
                break;
            }
        }
        // P2-RN03 / RF17
        s.setEstado(todoConStock ? "LISTA_DESPACHO" : "BORRADOR");
        solicitudRepo.save(s);
    }

    // ===== RF18 - listar faltantes =====
    @Transactional(readOnly = true)
    public List<Dtos.FaltanteDto> listarFaltantes(Long id) {
        Solicitud s = solicitudRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La solicitud no existe."));
        List<DetalleSolicitud> lineas = detalleRepo.findByIdSolicitud(s.getIdSolicitud());
        List<Dtos.FaltanteDto> faltantes = new ArrayList<>();
        for (DetalleSolicitud ds : lineas) {
            Material m = materialRepo.findById(ds.getIdMaterial()).orElse(null);
            if (m == null) continue;
            int saldo = ds.getCantidadSolicitada() - ds.getCantidadDespachada();
            if (saldo > 0 && m.getStockActual() < saldo) {
                faltantes.add(Dtos.FaltanteDto.builder()
                        .idMaterial(m.getIdMaterial())
                        .codigo(m.getCodigo())
                        .nombre(m.getNombre())
                        .cantidadSolicitada(saldo)
                        .stockActual(m.getStockActual())
                        .faltante(saldo - m.getStockActual())
                        .build());
            }
        }
        return faltantes;
    }

    // ===== Mapper a DTO =====
    @Transactional(readOnly = true)
    public Dtos.SolicitudDto toDto(Solicitud s) {
        Proyecto p = proyectoRepo.findById(s.getIdProyecto()).orElse(null);
        Usuario solicitante = usuarioRepo.findById(s.getIdUsuarioSolicitante()).orElse(null);
        Usuario aprobador = s.getIdUsuarioAprobador() == null ? null
                : usuarioRepo.findById(s.getIdUsuarioAprobador()).orElse(null);
        List<DetalleSolicitud> lineas = detalleRepo.findByIdSolicitud(s.getIdSolicitud());
        List<Dtos.DetalleSolicitudDto> detalle = lineas.stream().map(ds -> {
            Material m = materialRepo.findById(ds.getIdMaterial()).orElse(null);
            return Dtos.DetalleSolicitudDto.builder()
                    .idDetalleSolicitud(ds.getIdDetalleSolicitud())
                    .idMaterial(ds.getIdMaterial())
                    .codigoMaterial(m == null ? null : m.getCodigo())
                    .nombreMaterial(m == null ? null : m.getNombre())
                    .cantidadSolicitada(ds.getCantidadSolicitada())
                    .cantidadDespachada(ds.getCantidadDespachada())
                    .saldoPendiente(ds.getCantidadSolicitada() - ds.getCantidadDespachada())
                    .build();
        }).toList();
        return Dtos.SolicitudDto.builder()
                .idSolicitud(s.getIdSolicitud())
                .idProyecto(s.getIdProyecto())
                .codigoProyecto(p == null ? null : p.getCodigo())
                .cliente(p == null ? null : p.getCliente())
                .idUsuarioSolicitante(s.getIdUsuarioSolicitante())
                .solicitante(solicitante == null ? null : solicitante.getNombreCompleto())
                .idUsuarioAprobador(s.getIdUsuarioAprobador())
                .fechaSolicitud(s.getFechaSolicitud())
                .costoTotal(s.getCostoTotal())
                .presupuesto(p == null ? null : p.getPresupuesto())
                .estado(s.getEstado())
                .anulada(s.getAnulada())
                .motivoAnulacion(s.getMotivoAnulacion())
                .editable(!Boolean.TRUE.equals(s.getAnulada())
                        && despachoRepo.findByIdSolicitud(s.getIdSolicitud()).isEmpty()
                        && !"DESPACHADA".equals(s.getEstado()))
                .detalle(detalle)
                .build();
    }
}
