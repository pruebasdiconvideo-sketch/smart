package com.smartpoweron.gestion_materiales.service;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.entity.*;
import com.smartpoweron.gestion_materiales.exception.ReglaNegocioException;
import com.smartpoweron.gestion_materiales.repository.*;
import com.smartpoweron.gestion_materiales.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de despacho y entrega (RF19 a RF24, §7.2 - M3).
 *
 * Reglas aplicadas:
 *   P3-RN01 / R-INV-04 / RF21: el código y la cantidad digitados se validan
 *       contra el detalle de la solicitud; cantidad entregada ≤ saldo pendiente.
 *   P3-RN02 / RF22: despacho COMPLETO o PARCIAL según quede o no saldo.
 *   P3-RN03 / R-INV-03 / RF24: el stock se descuenta SOLO cuando el operario
 *       confirma la recepción; una segunda confirmación se rechaza (PF-29).
 *   R-INV-02: el descuento de stock y el movimiento SALIDA van en la misma
 *       transacción.
 */
@Service
public class DespachoService {

    private final DespachoRepository despachoRepo;
    private final DetalleDespachoRepository detalleDespRepo;
    private final DetalleSolicitudRepository detalleSolRepo;
    private final SolicitudRepository solicitudRepo;
    private final MaterialRepository materialRepo;
    private final EntregaRepository entregaRepo;
    private final UsuarioRepository usuarioRepo;
    private final ProyectoRepository proyectoRepo;
    private final TrazabilidadService trazabilidadService;
    private final SecurityUtil securityUtil;
    private final SolicitudService solicitudService;
    private final MaterialService materialService;

    public DespachoService(DespachoRepository despachoRepo,
                           DetalleDespachoRepository detalleDespRepo,
                           DetalleSolicitudRepository detalleSolRepo,
                           SolicitudRepository solicitudRepo,
                           MaterialRepository materialRepo,
                           EntregaRepository entregaRepo,
                           UsuarioRepository usuarioRepo,
                           ProyectoRepository proyectoRepo,
                           TrazabilidadService trazabilidadService,
                           SecurityUtil securityUtil,
                           SolicitudService solicitudService,
                           MaterialService materialService) {
        this.despachoRepo = despachoRepo;
        this.detalleDespRepo = detalleDespRepo;
        this.detalleSolRepo = detalleSolRepo;
        this.solicitudRepo = solicitudRepo;
        this.materialRepo = materialRepo;
        this.entregaRepo = entregaRepo;
        this.usuarioRepo = usuarioRepo;
        this.proyectoRepo = proyectoRepo;
        this.trazabilidadService = trazabilidadService;
        this.securityUtil = securityUtil;
        this.solicitudService = solicitudService;
        this.materialService = materialService;
    }

    // ===== RF19 - bandeja de despachos =====
    @Transactional(readOnly = true)
    public List<Dtos.BandejaItemDto> listarBandeja() {
        // LISTA_DESPACHO + despachos parciales con saldo pendiente
        List<Dtos.BandejaItemDto> items = new ArrayList<>();
        for (Solicitud s : solicitudRepo.findByEstado("LISTA_DESPACHO")) {
            if (Boolean.TRUE.equals(s.getAnulada())) continue;
            items.add(toBandeja(s, false));
        }
        // Despachos parciales: solicitudes con al menos una línea con saldo y despachos confirmados
        for (Solicitud s : solicitudRepo.findByEstadoIn(List.of("BORRADOR"))) {
            if (Boolean.TRUE.equals(s.getAnulada())) continue;
            boolean tieneSaldoParcial = detalleSolRepo.findByIdSolicitud(s.getIdSolicitud()).stream()
                    .anyMatch(ds -> ds.getCantidadDespachada() > 0
                            && ds.getCantidadDespachada() < ds.getCantidadSolicitada());
            if (tieneSaldoParcial) items.add(toBandeja(s, true));
        }
        return items;
    }

    private Dtos.BandejaItemDto toBandeja(Solicitud s, boolean esParcial) {
        Proyecto p = proyectoRepo.findById(s.getIdProyecto()).orElse(null);
        int totalPendiente = detalleSolRepo.findByIdSolicitud(s.getIdSolicitud()).stream()
                .mapToInt(ds -> ds.getCantidadSolicitada() - ds.getCantidadDespachada())
                .sum();
        return Dtos.BandejaItemDto.builder()
                .idSolicitud(s.getIdSolicitud())
                .codigoProyecto(p == null ? null : p.getCodigo())
                .cliente(p == null ? null : p.getCliente())
                .fechaSolicitud(s.getFechaSolicitud())
                .totalPendiente(totalPendiente)
                .esParcial(esParcial)
                .build();
    }

    // ===== RF21 - validar código y cantidad antes de registrar =====
    @Transactional(readOnly = true)
    public Dtos.ValidacionResponse validarCodigo(Dtos.ValidacionItemRequest req) {
        DetalleSolicitud ds = detalleSolRepo.findById(req.getIdDetalleSolicitud()).orElseThrow(() ->
                new ReglaNegocioException("El detalle de solicitud no existe."));
        Material m = materialRepo.findById(ds.getIdMaterial()).orElseThrow(() ->
                new ReglaNegocioException("El material del detalle no existe."));
        if (!m.getCodigo().equalsIgnoreCase(req.getCodigoDigitado())) {
            return Dtos.ValidacionResponse.builder()
                    .valido(false)
                    .mensaje("El código digitado no coincide con el solicitado.")
                    .codigoEsperado(m.getCodigo())
                    .build();
        }
        int saldo = ds.getCantidadSolicitada() - ds.getCantidadDespachada();
        if (req.getCantidadEntregada() > saldo) {
            return Dtos.ValidacionResponse.builder()
                    .valido(false)
                    .mensaje("La cantidad entregada supera el saldo pendiente (" + saldo + ").")
                    .codigoEsperado(m.getCodigo())
                    .build();
        }
        return Dtos.ValidacionResponse.builder()
                .valido(true)
                .mensaje("Validación correcta.")
                .codigoEsperado(m.getCodigo())
                .build();
    }

    // ===== RF20, RF21, RF22 - registrar despacho =====
    @Transactional
    public Dtos.DespachoDto registrarDespacho(Dtos.DespachoRequest req) {
        Usuario almacenero = securityUtil.usuarioOExcepcion();
        Solicitud s = solicitudRepo.findById(req.getIdSolicitud()).orElseThrow(() ->
                new ReglaNegocioException("La solicitud no existe."));
        if (Boolean.TRUE.equals(s.getAnulada())) {
            throw new ReglaNegocioException("La solicitud está anulada.");
        }
        if (!"LISTA_DESPACHO".equals(s.getEstado())) {
            // Sólo se despachan solicitudes LISTA_DESPACHO o parciales con saldo.
            List<DetalleSolicitud> lineas = detalleSolRepo.findByIdSolicitud(s.getIdSolicitud());
            boolean parcial = lineas.stream().anyMatch(ds ->
                    ds.getCantidadDespachada() > 0 && ds.getCantidadDespachada() < ds.getCantidadSolicitada());
            if (!parcial) {
                throw new ReglaNegocioException("La solicitud no está LISTA_DESPACHO.");
            }
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new ReglaNegocioException("El despacho debe contener al menos un ítem.");
        }
        // P3-RN01 / RF21: validar cada ítem
        List<DetalleDespacho> itemsAGuardar = new ArrayList<>();
        java.util.Set<Long> detallesIncluidos = new java.util.HashSet<>();
        for (Dtos.DespachoItemRequest item : req.getItems()) {
            DetalleSolicitud ds = detalleSolRepo.findById(item.getIdDetalleSolicitud()).orElseThrow(() ->
                    new ReglaNegocioException("El detalle de solicitud no existe."));
            if (!ds.getIdSolicitud().equals(s.getIdSolicitud())) {
                throw new ReglaNegocioException("El detalle no pertenece a la solicitud indicada.");
            }
            if (!detallesIncluidos.add(ds.getIdDetalleSolicitud())) {
                throw new ReglaNegocioException("No se puede incluir dos veces la misma línea en un despacho.");
            }
            Material m = materialRepo.findById(ds.getIdMaterial()).orElseThrow(() ->
                    new ReglaNegocioException("El material del detalle no existe."));
            if (!m.getCodigo().equalsIgnoreCase(item.getCodigoDigitado())) {
                throw new ReglaNegocioException(
                        "El código digitado '" + item.getCodigoDigitado() +
                                "' no coincide con el solicitado '" + m.getCodigo() + "'.");
            }
            int saldo = ds.getCantidadSolicitada() - ds.getCantidadDespachada();
            if (item.getCantidadEntregada() <= 0) {
                throw new ReglaNegocioException("La cantidad entregada debe ser mayor que cero.");
            }
            if (item.getCantidadEntregada() > saldo) {
                throw new ReglaNegocioException(
                        "La cantidad entregada (" + item.getCantidadEntregada() +
                                ") supera el saldo pendiente (" + saldo + ") de " + m.getCodigo() + ".");
            }
            itemsAGuardar.add(DetalleDespacho.builder()
                    .idDetalleSolicitud(ds.getIdDetalleSolicitud())
                    .idMaterial(m.getIdMaterial())
                    .codigoDigitado(item.getCodigoDigitado())
                    .cantidadEntregada(item.getCantidadEntregada())
                    .build());
        }
        // P3-RN02 / RF22: ¿es COMPLETO o PARCIAL?
        List<DetalleSolicitud> lineas = detalleSolRepo.findByIdSolicitud(s.getIdSolicitud());
        int saldoTotalAntes = lineas.stream()
                .mapToInt(ds -> ds.getCantidadSolicitada() - ds.getCantidadDespachada()).sum();
        int entregadoAhora = itemsAGuardar.stream().mapToInt(DetalleDespacho::getCantidadEntregada).sum();
        String tipo = (entregadoAhora >= saldoTotalAntes) ? "COMPLETO" : "PARCIAL";

        Despacho d = Despacho.builder()
                .idSolicitud(s.getIdSolicitud())
                .idUsuarioAlmacenero(almacenero.getIdUsuario())
                .fechaDespacho(LocalDateTime.now())
                .tipo(tipo)
                .estado("PENDIENTE_CONFIRMACION")
                .build();
        d = despachoRepo.save(d);
        for (DetalleDespacho dd : itemsAGuardar) {
            dd.setIdDespacho(d.getIdDespacho());
            detalleDespRepo.save(dd);
        }
        return toDto(d);
    }

    @Transactional(readOnly = true)
    public Dtos.DespachoDto obtenerDespacho(Long idDespacho) {
        Despacho despacho = despachoRepo.findById(idDespacho).orElseThrow(() ->
                new ReglaNegocioException("El despacho no existe."));
        return toDto(despacho);
    }

    @Transactional
    public Dtos.DespachoDto actualizarDespacho(Long idDespacho, Dtos.DespachoRequest req) {
        Despacho despacho = despachoRepo.findById(idDespacho).orElseThrow(() ->
                new ReglaNegocioException("El despacho no existe."));
        if (Boolean.TRUE.equals(despacho.getAnulado()) || !"PENDIENTE_CONFIRMACION".equals(despacho.getEstado())) {
            throw new ReglaNegocioException("Sólo se pueden editar despachos activos pendientes de confirmación.");
        }
        if (!despacho.getIdSolicitud().equals(req.getIdSolicitud())) {
            throw new ReglaNegocioException("No se puede cambiar la solicitud asociada al despacho.");
        }
        Solicitud solicitud = solicitudRepo.findById(despacho.getIdSolicitud()).orElseThrow(() ->
                new ReglaNegocioException("La solicitud no existe."));
        if (Boolean.TRUE.equals(solicitud.getAnulada())) {
            throw new ReglaNegocioException("La solicitud está anulada.");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new ReglaNegocioException("El despacho debe contener al menos un ítem.");
        }
        List<DetalleDespacho> nuevosDetalles = new ArrayList<>();
        java.util.Set<Long> incluidos = new java.util.HashSet<>();
        for (Dtos.DespachoItemRequest item : req.getItems()) {
            DetalleSolicitud linea = detalleSolRepo.findById(item.getIdDetalleSolicitud()).orElseThrow(() ->
                    new ReglaNegocioException("El detalle de solicitud no existe."));
            if (!linea.getIdSolicitud().equals(solicitud.getIdSolicitud())) {
                throw new ReglaNegocioException("El detalle no pertenece a la solicitud indicada.");
            }
            if (!incluidos.add(linea.getIdDetalleSolicitud())) {
                throw new ReglaNegocioException("No se puede incluir dos veces la misma línea en un despacho.");
            }
            Material material = materialRepo.findById(linea.getIdMaterial()).orElseThrow(() ->
                    new ReglaNegocioException("El material del detalle no existe."));
            if (!material.getCodigo().equalsIgnoreCase(item.getCodigoDigitado())) {
                throw new ReglaNegocioException("El código digitado no coincide con el material solicitado.");
            }
            int saldo = linea.getCantidadSolicitada() - linea.getCantidadDespachada();
            if (item.getCantidadEntregada() == null || item.getCantidadEntregada() <= 0
                    || item.getCantidadEntregada() > saldo) {
                throw new ReglaNegocioException("La cantidad entregada debe ser mayor que cero y no superar el saldo pendiente.");
            }
            nuevosDetalles.add(DetalleDespacho.builder()
                    .idDespacho(despacho.getIdDespacho())
                    .idDetalleSolicitud(linea.getIdDetalleSolicitud())
                    .idMaterial(material.getIdMaterial())
                    .codigoDigitado(item.getCodigoDigitado())
                    .cantidadEntregada(item.getCantidadEntregada())
                    .build());
        }
        int saldoTotal = detalleSolRepo.findByIdSolicitud(solicitud.getIdSolicitud()).stream()
                .mapToInt(linea -> linea.getCantidadSolicitada() - linea.getCantidadDespachada()).sum();
        int entregado = nuevosDetalles.stream().mapToInt(DetalleDespacho::getCantidadEntregada).sum();
        detalleDespRepo.deleteAll(detalleDespRepo.findByIdDespacho(idDespacho));
        detalleDespRepo.saveAll(nuevosDetalles);
        despacho.setTipo(entregado >= saldoTotal ? "COMPLETO" : "PARCIAL");
        return toDto(despachoRepo.save(despacho));
    }

    // ===== RF23, RF24 - confirmar recepción y descontar stock =====
    @Transactional
    public Dtos.DespachoDto confirmarEntrega(Long idDespacho) {
        Usuario operario = securityUtil.usuarioOExcepcion();
        Despacho d = despachoRepo.findById(idDespacho).orElseThrow(() ->
                new ReglaNegocioException("El despacho no existe."));
        if (!"PENDIENTE_CONFIRMACION".equals(d.getEstado())) {
            throw new ReglaNegocioException("Este despacho ya fue confirmado."); // PF-29
        }
        if (Boolean.TRUE.equals(d.getAnulado())) {
            throw new ReglaNegocioException("Este despacho está anulado.");
        }
        Solicitud s = solicitudRepo.findById(d.getIdSolicitud()).orElseThrow(() ->
                new ReglaNegocioException("La solicitud del despacho no existe."));
        if (Boolean.TRUE.equals(s.getAnulada())) {
            throw new ReglaNegocioException("La solicitud del despacho está anulada.");
        }

        // Procesar cada ítem: descontar stock + actualizar despachado + trazabilidad (R-INV-02)
        List<DetalleDespacho> items = detalleDespRepo.findByIdDespacho(d.getIdDespacho());
        for (DetalleDespacho dd : items) {
            Material m = materialRepo.findById(dd.getIdMaterial()).orElseThrow(() ->
                    new ReglaNegocioException("El material del despacho no existe."));
            // R-INV-03: stock no negativo
            if (m.getStockActual() < dd.getCantidadEntregada()) {
                throw new ReglaNegocioException(
                        "Stock insuficiente para " + m.getCodigo() +
                                " (stock " + m.getStockActual() + ", entrega " + dd.getCantidadEntregada() + ").");
            }
            m.setStockActual(m.getStockActual() - dd.getCantidadEntregada());
            materialRepo.save(m);
            // Trazabilidad individual: marcar instancias como SALIDA (FIFO best-effort)
            materialService.marcarSalidaInstancias(m.getIdMaterial(), dd.getCantidadEntregada());
            // Actualizar cantidad despachada de la línea
            DetalleSolicitud ds = detalleSolRepo.findById(dd.getIdDetalleSolicitud()).orElseThrow();
            if (ds.getCantidadDespachada() + dd.getCantidadEntregada() > ds.getCantidadSolicitada()) {
                throw new ReglaNegocioException("La entrega supera el saldo pendiente de la solicitud.");
            }
            ds.setCantidadDespachada(ds.getCantidadDespachada() + dd.getCantidadEntregada());
            detalleSolRepo.save(ds);
            // Trazabilidad SALIDA en la misma transacción (R-INV-02)
            trazabilidadService.registrarMovimiento(
                    m.getIdMaterial(), operario.getIdUsuario(), s.getIdProyecto(), null,
                    "SALIDA", dd.getCantidadEntregada());
        }
        // Marcar despacho confirmado y registrar entrega
        d.setEstado("CONFIRMADO");
        despachoRepo.save(d);
        Entrega e = Entrega.builder()
                .idDespacho(d.getIdDespacho())
                .idUsuarioOperario(operario.getIdUsuario())
                .fechaConfirmacion(LocalDateTime.now())
                .confirmado(true)
                .build();
        entregaRepo.save(e);

        // Si todas las líneas están completas → solicitud DESPACHADA
        List<DetalleSolicitud> lineas = detalleSolRepo.findByIdSolicitud(s.getIdSolicitud());
        boolean completa = lineas.stream().allMatch(ds ->
                ds.getCantidadDespachada() >= ds.getCantidadSolicitada());
        if (completa) {
            s.setEstado("DESPACHADA");
            solicitudRepo.save(s);
        }
        return toDto(d);
    }

    // ===== RF23 - bandeja de pendientes de confirmación (operario) =====
    @Transactional(readOnly = true)
    public List<Dtos.PendienteConfirmacionDto> listarPendientesConfirmacion() {
        return despachoRepo.findByEstado("PENDIENTE_CONFIRMACION").stream()
                .filter(d -> !Boolean.TRUE.equals(d.getAnulado()))
                .map(d -> {
                    Solicitud s = solicitudRepo.findById(d.getIdSolicitud()).orElse(null);
                    Proyecto p = s == null ? null : proyectoRepo.findById(s.getIdProyecto()).orElse(null);
                    int total = detalleDespRepo.findByIdDespacho(d.getIdDespacho()).stream()
                            .mapToInt(DetalleDespacho::getCantidadEntregada).sum();
                    return Dtos.PendienteConfirmacionDto.builder()
                            .idDespacho(d.getIdDespacho())
                            .idSolicitud(d.getIdSolicitud())
                            .codigoProyecto(p == null ? null : p.getCodigo())
                            .fechaDespacho(d.getFechaDespacho())
                            .tipo(d.getTipo())
                            .totalEntregado(total)
                            .build();
                }).toList();
    }

    @Transactional
    public Dtos.DespachoDto anular(Long idDespacho, String motivo) {
        Usuario actor = securityUtil.usuarioOExcepcion();
        Despacho despacho = despachoRepo.findById(idDespacho).orElseThrow(() ->
                new ReglaNegocioException("El despacho no existe."));
        if (Boolean.TRUE.equals(despacho.getAnulado())) {
            throw new ReglaNegocioException("El despacho ya está anulado.");
        }
        Solicitud solicitud = solicitudRepo.findById(despacho.getIdSolicitud()).orElseThrow(() ->
                new ReglaNegocioException("La solicitud del despacho no existe."));
        if (Boolean.TRUE.equals(solicitud.getAnulada())) {
            throw new ReglaNegocioException("La solicitud ya está anulada.");
        }
        String rol = securityUtil.rolActual().getNombre();
        if ("OPERARIO_ENSAMBLE".equals(rol) && "CONFIRMADO".equals(despacho.getEstado())) {
            throw new ReglaNegocioException("El operario sólo puede anular despachos pendientes de confirmación.");
        }
        if ("CONFIRMADO".equals(despacho.getEstado())) {
            boolean otroPendiente = despachoRepo.findByIdSolicitud(solicitud.getIdSolicitud()).stream()
                    .anyMatch(otro -> !otro.getIdDespacho().equals(idDespacho)
                            && !Boolean.TRUE.equals(otro.getAnulado())
                            && "PENDIENTE_CONFIRMACION".equals(otro.getEstado()));
            if (otroPendiente) {
                throw new ReglaNegocioException("Anule o confirme los otros despachos pendientes antes de compensar éste.");
            }
            for (DetalleDespacho detalle : detalleDespRepo.findByIdDespacho(idDespacho)) {
                Material material = materialRepo.findById(detalle.getIdMaterial()).orElseThrow(() ->
                        new ReglaNegocioException("No se encontró un material del despacho para compensarlo."));
                DetalleSolicitud linea = detalleSolRepo.findById(detalle.getIdDetalleSolicitud()).orElseThrow(() ->
                        new ReglaNegocioException("No se encontró una línea de solicitud para compensarla."));
                if (linea.getCantidadDespachada() < detalle.getCantidadEntregada()) {
                    throw new ReglaNegocioException("La cantidad despachada no coincide con el historial; no se puede anular.");
                }
                material.setStockActual(material.getStockActual() + detalle.getCantidadEntregada());
                linea.setCantidadDespachada(linea.getCantidadDespachada() - detalle.getCantidadEntregada());
                materialRepo.save(material);
                detalleSolRepo.save(linea);
                trazabilidadService.registrarMovimiento(material.getIdMaterial(), actor.getIdUsuario(),
                        solicitud.getIdProyecto(), null, "DEVOLUCION", detalle.getCantidadEntregada());
            }
        }
        despacho.setAnulado(true);
        despacho.setMotivoAnulacion(motivo.trim());
        despachoRepo.save(despacho);
        solicitudService.recalcularEstadoDespuesDeCompensacion(solicitud.getIdSolicitud());
        return toDto(despacho);
    }

    // ===== Mapper =====
    @Transactional(readOnly = true)
    public Dtos.DespachoDto toDto(Despacho d) {
        Solicitud s = solicitudRepo.findById(d.getIdSolicitud()).orElse(null);
        Proyecto p = s == null ? null : proyectoRepo.findById(s.getIdProyecto()).orElse(null);
        List<DetalleDespacho> items = detalleDespRepo.findByIdDespacho(d.getIdDespacho());
        List<Dtos.ItemDespachoDto> itemsDto = items.stream().map(dd -> {
            DetalleSolicitud ds = detalleSolRepo.findById(dd.getIdDetalleSolicitud()).orElse(null);
            Material m = materialRepo.findById(dd.getIdMaterial()).orElse(null);
            return Dtos.ItemDespachoDto.builder()
                    .idDetalleDespacho(dd.getIdDetalleDespacho())
                    .idDetalleSolicitud(dd.getIdDetalleSolicitud())
                    .codigoDigitado(dd.getCodigoDigitado())
                    .codigoEsperado(m == null ? null : m.getCodigo())
                    .cantidadEntregada(dd.getCantidadEntregada())
                    .build();
        }).toList();
        return Dtos.DespachoDto.builder()
                .idDespacho(d.getIdDespacho())
                .idSolicitud(d.getIdSolicitud())
                .codigoProyecto(p == null ? null : p.getCodigo())
                .tipo(d.getTipo())
                .estado(d.getEstado())
                .anulado(d.getAnulado())
                .motivoAnulacion(d.getMotivoAnulacion())
                .fechaDespacho(d.getFechaDespacho())
                .items(itemsDto)
                .build();
    }
}
