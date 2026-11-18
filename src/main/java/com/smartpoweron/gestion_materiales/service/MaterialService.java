package com.smartpoweron.gestion_materiales.service;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.entity.Material;
import com.smartpoweron.gestion_materiales.entity.OrdenCompra;
import com.smartpoweron.gestion_materiales.entity.Usuario;
import com.smartpoweron.gestion_materiales.exception.ReglaNegocioException;
import com.smartpoweron.gestion_materiales.repository.MaterialRepository;
import com.smartpoweron.gestion_materiales.repository.OrdenCompraRepository;
import com.smartpoweron.gestion_materiales.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de inventario y catálogo (RF05 a RF11, §7.2 - M1).
 *
 * Reglas de negocio aplicadas:
 *   P1-RN01 (RF05): código de fabricante único + especificación técnica.
 *   P1-RN02 / R-INV-05 (RF09): ingreso sólo si código existe, OC ABIERTA y
 *                               cantidad > 0; si no, se rechaza y el stock no cambia.
 *   R-INV-02 (RF10): el incremento de stock y el movimiento ENTRADA van en la
 *                    MISMA transacción.
 *   P1-RN03 (RF11): alerta cuando stock_actual <= stock_minimo.
 */
@Service
public class MaterialService {

    private final MaterialRepository materialRepo;
    private final OrdenCompraRepository ocRepo;
    private final TrazabilidadService trazabilidadService;
    private final SecurityUtil securityUtil;

    public MaterialService(MaterialRepository materialRepo, OrdenCompraRepository ocRepo,
                           TrazabilidadService trazabilidadService, SecurityUtil securityUtil) {
        this.materialRepo = materialRepo;
        this.ocRepo = ocRepo;
        this.trazabilidadService = trazabilidadService;
        this.securityUtil = securityUtil;
    }

    // ===== RF05 - registrar material =====
    @Transactional
    public Dtos.MaterialDto registrarMaterial(Dtos.MaterialCreate req) {
        if (materialRepo.existsByCodigo(req.getCodigo())) {
            throw new ReglaNegocioException("El código ya se encuentra registrado");
        }
        if (req.getStockMinimo() == null || req.getStockMinimo() < 0) {
            throw new ReglaNegocioException("El stock mínimo debe ser un número no negativo.");
        }
        Material m = Material.builder()
                .codigo(req.getCodigo())
                .nombre(req.getNombre())
                .especificacionTecnica(req.getEspecificacionTecnica())
                .descripcion(req.getDescripcion())
                .categoria(req.getCategoria())
                .marca(req.getMarca())
                .unidadMedida(req.getUnidadMedida())
                .modelo(req.getModelo())
                .numeroSerie(req.getNumeroSerie())
                .mlfb(req.getMlfb())
                .pasillo(req.getPasillo())
                .estante(req.getEstante())
                .nivel(req.getNivel())
                .precioUnitario(req.getPrecioUnitario())
                .stockActual(0)
                .stockMinimo(req.getStockMinimo() == null ? 0 : req.getStockMinimo())
                .estado("ACTIVO")
                .build();
        return toDto(materialRepo.save(m));
    }

    // ===== RF06 - consultar catálogo con filtros =====
    @Transactional(readOnly = true)
    public List<Dtos.MaterialDto> consultarCatalogo(String codigo, String nombre, String categoria,
                                                     boolean incluirInactivos) {
        List<Material> all = materialRepo.findAll();
        return all.stream()
                .filter(m -> incluirInactivos || "ACTIVO".equals(m.getEstado()))
                .filter(m -> codigo == null || codigo.isBlank()
                        || m.getCodigo().toLowerCase().contains(codigo.toLowerCase()))
                .filter(m -> nombre == null || nombre.isBlank()
                        || m.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .filter(m -> categoria == null || categoria.isBlank()
                        || (m.getCategoria() != null && m.getCategoria().toLowerCase().contains(categoria.toLowerCase())))
                .map(this::toDto)
                .toList();
    }

    // ===== RF07 - actualizar datos del material (no el stock) =====
    @Transactional
    public Dtos.MaterialDto actualizarMaterial(Long id, Dtos.MaterialUpdate req) {
        Material m = materialRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("El material no existe."));
        if (!"ACTIVO".equals(m.getEstado())) {
            throw new ReglaNegocioException("No se puede editar un material inactivo.");
        }
        if (materialRepo.existsByCodigoAndIdMaterialNot(req.getCodigo(), id)) {
            throw new ReglaNegocioException("El código ya se encuentra registrado.");
        }
        if (req.getStockMinimo() != null && req.getStockMinimo() < 0) {
            throw new ReglaNegocioException("El stock mínimo debe ser un número no negativo.");
        }
        m.setCodigo(req.getCodigo());
        m.setNombre(req.getNombre());
        m.setEspecificacionTecnica(req.getEspecificacionTecnica());
        m.setDescripcion(req.getDescripcion());
        m.setCategoria(req.getCategoria());
        m.setMarca(req.getMarca());
        m.setUnidadMedida(req.getUnidadMedida());
        m.setModelo(req.getModelo());
        m.setNumeroSerie(req.getNumeroSerie());
        m.setMlfb(req.getMlfb());
        m.setPasillo(req.getPasillo());
        m.setEstante(req.getEstante());
        m.setNivel(req.getNivel());
        m.setPrecioUnitario(req.getPrecioUnitario());
        if (req.getStockMinimo() != null) {
            m.setStockMinimo(req.getStockMinimo());
        }
        return toDto(materialRepo.save(m));
    }

    @Transactional
    public void desactivarMaterial(Long id) {
        Material m = materialRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("El material no existe."));
        if (!"ACTIVO".equals(m.getEstado())) {
            throw new ReglaNegocioException("El material ya está inactivo.");
        }
        m.setEstado("INACTIVO");
        materialRepo.save(m);
    }

    @Transactional
    public void reactivarMaterial(Long id) {
        Material m = materialRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("El material no existe."));
        if ("ACTIVO".equals(m.getEstado())) {
            throw new ReglaNegocioException("El material ya está activo.");
        }
        m.setEstado("ACTIVO");
        materialRepo.save(m);
    }

    // ===== RF08, RF09, RF10 - ingreso contra orden de compra =====
    @Transactional
    public Dtos.IngresoResponse registrarIngreso(Dtos.IngresoRequest req) {
        Usuario u = securityUtil.usuarioOExcepcion();
        OrdenCompra oc = ocRepo.findById(req.getIdOrdenCompra()).orElseThrow(() ->
                new ReglaNegocioException("La orden de compra no existe."));
        // R-INV-05: la OC debe estar ABIERTA
        if (!"ABIERTA".equals(oc.getEstado())) {
            throw new ReglaNegocioException("La orden de compra no está ABIERTA.");
        }
        List<String> detalle = new ArrayList<>();
        List<String> noRegistrados = new ArrayList<>();

        for (Dtos.IngresoItem item : req.getItems()) {
            // P1-RN02: cantidad > 0
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new ReglaNegocioException("La cantidad debe ser mayor que cero.");
            }
            Optional<Material> opt = materialRepo.findByCodigo(item.getCodigo());
            if (opt.isEmpty()) {
                // P1-T05: el material debe registrarse primero (no se crea solo).
                noRegistrados.add(item.getCodigo());
                continue;
            }
            Material m = opt.get();
            if (!"ACTIVO".equals(m.getEstado())) {
                throw new ReglaNegocioException("El material " + m.getCodigo() + " está inactivo.");
            }
            // R-INV-02: stock + trazabilidad en la misma transacción
            m.setStockActual(m.getStockActual() + item.getCantidad());
            materialRepo.save(m);
            trazabilidadService.registrarMovimiento(
                    m.getIdMaterial(), u.getIdUsuario(), null, oc.getIdOrdenCompra(),
                    "ENTRADA", item.getCantidad());
            detalle.add(m.getCodigo() + ": +" + item.getCantidad() + " -> stock " + m.getStockActual());
        }
        boolean exito = noRegistrados.isEmpty();
        String mensaje = exito
                ? "Ingreso registrado correctamente."
                : "Algunos códigos no existen en el catálogo; regístrelos primero (P1-T05).";
        return Dtos.IngresoResponse.builder()
                .exito(exito)
                .mensaje(mensaje)
                .detalle(detalle)
                .materialesNoRegistrados(noRegistrados)
                .build();
    }

    // ===== RF08 - listar órdenes de compra abiertas =====
    @Transactional(readOnly = true)
    public List<Dtos.OrdenCompraDto> listarOrdenes(boolean incluirCerradas) {
        List<OrdenCompra> ordenes = incluirCerradas ? ocRepo.findAll() : ocRepo.findByEstado("ABIERTA");
        return ordenes.stream()
                .map(oc -> Dtos.OrdenCompraDto.builder()
                        .idOrdenCompra(oc.getIdOrdenCompra())
                        .codigo(oc.getCodigo())
                        .proveedor(oc.getProveedor())
                        .fechaEmision(oc.getFechaEmision())
                        .estado(oc.getEstado())
                        .build())
                .toList();
    }

    @Transactional
    public Dtos.OrdenCompraDto crearOrden(Dtos.OrdenCompraCreate req) {
        if (ocRepo.findByCodigo(req.getCodigo()).isPresent()) {
            throw new ReglaNegocioException("El código de orden ya está registrado.");
        }
        OrdenCompra orden = OrdenCompra.builder()
                .codigo(req.getCodigo())
                .proveedor(req.getProveedor())
                .fechaEmision(req.getFechaEmision())
                .estado("ABIERTA")
                .build();
        return toOrdenDto(ocRepo.save(orden));
    }

    @Transactional
    public Dtos.OrdenCompraDto actualizarOrden(Long id, Dtos.OrdenCompraUpdate req) {
        OrdenCompra orden = ocRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La orden de compra no existe."));
        if (!"ABIERTA".equals(orden.getEstado())) {
            throw new ReglaNegocioException("No se puede editar una orden cerrada.");
        }
        if (ocRepo.existsByCodigoAndIdOrdenCompraNot(req.getCodigo(), id)) {
            throw new ReglaNegocioException("El código de orden ya está registrado.");
        }
        orden.setCodigo(req.getCodigo());
        orden.setProveedor(req.getProveedor());
        orden.setFechaEmision(req.getFechaEmision());
        return toOrdenDto(ocRepo.save(orden));
    }

    @Transactional
    public void cerrarOrden(Long id) {
        OrdenCompra orden = ocRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La orden de compra no existe."));
        if (!"ABIERTA".equals(orden.getEstado())) {
            throw new ReglaNegocioException("La orden ya está cerrada.");
        }
        orden.setEstado("CERRADA");
        ocRepo.save(orden);
    }

    @Transactional
    public void reabrirOrden(Long id) {
        OrdenCompra orden = ocRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La orden de compra no existe."));
        if ("ABIERTA".equals(orden.getEstado())) {
            throw new ReglaNegocioException("La orden ya está abierta.");
        }
        orden.setEstado("ABIERTA");
        ocRepo.save(orden);
    }

    private Dtos.OrdenCompraDto toOrdenDto(OrdenCompra orden) {
        return Dtos.OrdenCompraDto.builder()
                .idOrdenCompra(orden.getIdOrdenCompra())
                .codigo(orden.getCodigo())
                .proveedor(orden.getProveedor())
                .fechaEmision(orden.getFechaEmision())
                .estado(orden.getEstado())
                .build();
    }

    // ===== RF11 - alertas de stock mínimo =====
    @Transactional(readOnly = true)
    public List<Dtos.MaterialDto> listarAlertas() {
        return materialRepo.findAll().stream()
                .filter(m -> "ACTIVO".equals(m.getEstado()))
                .filter(m -> m.getStockActual() <= m.getStockMinimo())
                .map(this::toDto)
                .toList();
    }

    // ===== Compatibilidad con vistas Thymeleaf antiguas =====
    @Transactional(readOnly = true)
    public List<Material> listarMateriales() {
        return materialRepo.findAll().stream()
                .filter(m -> "ACTIVO".equals(m.getEstado()))
                .toList();
    }

    @Transactional
    public void registrarOActualizarIngreso(Material material, Integer cantidadIngresada) {
        if (cantidadIngresada == null || cantidadIngresada <= 0) {
            throw new ReglaNegocioException("La cantidad ingresada debe ser mayor que cero.");
        }

        Optional<Material> existente = materialRepo.findByCodigo(material.getCodigo());
        if (existente.isPresent()) {
            Material actual = existente.get();
            if (!"ACTIVO".equals(actual.getEstado())) {
                throw new ReglaNegocioException("No se puede registrar un ingreso para un material inactivo.");
            }
            actual.setNombre(material.getNombre());
            actual.setEspecificacionTecnica(material.getDescripcion());
            actual.setDescripcion(material.getDescripcion());
            actual.setCategoria(material.getCategoria());
            actual.setMarca(material.getMarca());
            actual.setUnidadMedida(material.getUnidadMedida());
            actual.setModelo(material.getModelo());
            actual.setNumeroSerie(material.getNumeroSerie());
            actual.setMlfb(material.getMlfb());
            actual.setPasillo(material.getPasillo());
            actual.setEstante(material.getEstante());
            actual.setNivel(material.getNivel());
            actual.setStockMinimo(material.getStockMinimo());
            actual.setStockActual(actual.getStockActual() + cantidadIngresada);
            materialRepo.save(actual);
            return;
        }

        material.setEspecificacionTecnica(material.getDescripcion());
        material.setStockActual(cantidadIngresada);
        if (material.getEstado() == null) {
            material.setEstado("ACTIVO");
        }
        if (material.getStockMinimo() == null) {
            material.setStockMinimo(0);
        }
        if (material.getPrecioUnitario() == null) {
            material.setPrecioUnitario(java.math.BigDecimal.ZERO);
        }
        if (material.getUnidadMedida() == null || material.getUnidadMedida().isBlank()) {
            material.setUnidadMedida("Unidades");
        }
        materialRepo.save(material);
    }

    // ===== Helpers =====
    public Dtos.MaterialDto toDto(Material m) {
        return Dtos.MaterialDto.builder()
                .idMaterial(m.getIdMaterial())
                .codigo(m.getCodigo())
                .nombre(m.getNombre())
                .especificacionTecnica(m.getEspecificacionTecnica())
                .descripcion(m.getDescripcion())
                .categoria(m.getCategoria())
                .marca(m.getMarca())
                .unidadMedida(m.getUnidadMedida())
                .modelo(m.getModelo())
                .numeroSerie(m.getNumeroSerie())
                .mlfb(m.getMlfb())
                .pasillo(m.getPasillo())
                .estante(m.getEstante())
                .nivel(m.getNivel())
                .estado(m.getEstado())
                .precioUnitario(m.getPrecioUnitario())
                .stockActual(m.getStockActual())
                .stockMinimo(m.getStockMinimo())
                .enAlerta(m.getStockActual() <= m.getStockMinimo())
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<Material> buscarPorCodigo(String codigo) {
        return materialRepo.findByCodigo(codigo);
    }
}
