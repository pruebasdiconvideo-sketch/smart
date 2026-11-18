package com.smartpoweron.gestion_materiales.service;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.entity.DetalleOrdenCompra;
import com.smartpoweron.gestion_materiales.entity.Empresa;
import com.smartpoweron.gestion_materiales.entity.Material;
import com.smartpoweron.gestion_materiales.entity.MaterialInstancia;
import com.smartpoweron.gestion_materiales.entity.OrdenCompra;
import com.smartpoweron.gestion_materiales.entity.Usuario;
import com.smartpoweron.gestion_materiales.exception.ReglaNegocioException;
import com.smartpoweron.gestion_materiales.repository.DetalleOrdenCompraRepository;
import com.smartpoweron.gestion_materiales.repository.EmpresaRepository;
import com.smartpoweron.gestion_materiales.repository.MaterialInstanciaRepository;
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
    private final DetalleOrdenCompraRepository detalleOcRepo;
    private final EmpresaRepository empresaRepo;
    private final MaterialInstanciaRepository instanciaRepo;
    private final TrazabilidadService trazabilidadService;
    private final SecurityUtil securityUtil;

    public MaterialService(MaterialRepository materialRepo, OrdenCompraRepository ocRepo,
                           DetalleOrdenCompraRepository detalleOcRepo, EmpresaRepository empresaRepo,
                           MaterialInstanciaRepository instanciaRepo,
                           TrazabilidadService trazabilidadService, SecurityUtil securityUtil) {
        this.materialRepo = materialRepo;
        this.ocRepo = ocRepo;
        this.detalleOcRepo = detalleOcRepo;
        this.empresaRepo = empresaRepo;
        this.instanciaRepo = instanciaRepo;
        this.trazabilidadService = trazabilidadService;
        this.securityUtil = securityUtil;
    }

    // ===== RF05 - registrar material =====
    @Transactional
    public Dtos.MaterialDto registrarMaterial(Dtos.MaterialCreate req) {
        // Código automático según categoría cuando no se proporciona
        String codigo = (req.getCodigo() == null || req.getCodigo().isBlank())
                ? siguienteCodigo(req.getCategoria())
                : req.getCodigo().trim();
        if (materialRepo.existsByCodigo(codigo)) {
            throw new ReglaNegocioException("El código ya se encuentra registrado: " + codigo);
        }
        if (req.getStockMinimo() == null || req.getStockMinimo() < 0) {
            throw new ReglaNegocioException("El stock mínimo debe ser un número no negativo.");
        }
        Material m = Material.builder()
                .codigo(codigo)
                .nombre(req.getNombre())
                .especificacionTecnica(req.getEspecificacionTecnica())
                .descripcion(req.getDescripcion())
                .categoria(req.getCategoria())
                .marca(req.getMarca())
                .unidadMedida(req.getUnidadMedida())
                .modelo(req.getModelo())
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
        String codigoFinal = (req.getCodigo() == null || req.getCodigo().isBlank())
                ? m.getCodigo() : req.getCodigo().trim();
        if (materialRepo.existsByCodigoAndIdMaterialNot(codigoFinal, id)) {
            throw new ReglaNegocioException("El código ya se encuentra registrado.");
        }
        if (req.getStockMinimo() != null && req.getStockMinimo() < 0) {
            throw new ReglaNegocioException("El stock mínimo debe ser un número no negativo.");
        }
        m.setCodigo(codigoFinal);
        m.setNombre(req.getNombre());
        m.setEspecificacionTecnica(req.getEspecificacionTecnica());
        m.setDescripcion(req.getDescripcion());
        m.setCategoria(req.getCategoria());
        m.setMarca(req.getMarca());
        m.setUnidadMedida(req.getUnidadMedida());
        m.setModelo(req.getModelo());
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
            List<String> series = item.getNumeroSeries();
            boolean conSeries = series != null && !series.isEmpty();
            // Cantidad efectiva: una por serie si se proveen, si no la cantidad indicada
            int cantidad = conSeries ? series.size() : (item.getCantidad() == null ? 0 : item.getCantidad());
            if (cantidad <= 0) {
                throw new ReglaNegocioException("La cantidad debe ser mayor que cero (ingrese series o una cantidad).");
            }
            Optional<Material> opt = materialRepo.findByCodigo(item.getCodigo());
            if (opt.isEmpty()) {
                // El material debe registrarse primero en el catálogo (no se crea solo).
                noRegistrados.add(item.getCodigo());
                continue;
            }
            Material m = opt.get();
            if (!"ACTIVO".equals(m.getEstado())) {
                throw new ReglaNegocioException("El material " + m.getCodigo() + " está inactivo.");
            }
            // Informe: "Número de serie único por cada producto" — validar y registrar una instancia por unidad
            if (conSeries) {
                java.util.Set<String> vistas = new java.util.HashSet<>();
                for (String s : series) {
                    String serie = s == null ? "" : s.trim();
                    if (serie.isEmpty()) {
                        throw new ReglaNegocioException("Hay un número de serie vacío para " + m.getCodigo() + ".");
                    }
                    if (!vistas.add(serie)) {
                        throw new ReglaNegocioException("Número de serie duplicado en el ingreso: " + serie);
                    }
                    if (instanciaRepo.existsByNumeroSerie(serie)) {
                        throw new ReglaNegocioException("El número de serie " + serie + " ya existe para " + m.getCodigo() + ".");
                    }
                    instanciaRepo.save(MaterialInstancia.builder()
                            .idMaterial(m.getIdMaterial())
                            .numeroSerie(serie)
                            .idOrdenCompra(oc.getIdOrdenCompra())
                            .idUsuario(u.getIdUsuario())
                            .fechaIngreso(java.time.LocalDateTime.now())
                            .estado("EN_ALMACEN")
                            .build());
                }
            }
            // R-INV-02: stock + trazabilidad en la MISMA transacción
            m.setStockActual(m.getStockActual() + cantidad);
            materialRepo.save(m);
            trazabilidadService.registrarMovimiento(
                    m.getIdMaterial(), u.getIdUsuario(), null, oc.getIdOrdenCompra(),
                    "ENTRADA", cantidad);
            detalle.add(m.getCodigo() + ": +" + cantidad + " -> stock " + m.getStockActual()
                    + (conSeries ? " (" + series.size() + " serie(s) registradas)" : ""));
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

    // ===== Trazabilidad individual por número de serie (informe) =====
    @Transactional(readOnly = true)
    public Dtos.MaterialInstanciaDto buscarInstanciaPorSerie(String numeroSerie) {
        if (numeroSerie == null || numeroSerie.isBlank()) return null;
        return instanciaRepo.findByNumeroSerie(numeroSerie.trim())
                .map(this::toInstanciaDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Dtos.MaterialInstanciaDto> listarInstanciasPorMaterial(Long idMaterial) {
        return instanciaRepo.findByIdMaterialOrderByIdInstanciaDesc(idMaterial).stream()
                .map(this::toInstanciaDto)
                .toList();
    }

    private Dtos.MaterialInstanciaDto toInstanciaDto(MaterialInstancia ins) {
        Material m = materialRepo.findById(ins.getIdMaterial()).orElse(null);
        String codigoOC = null;
        if (ins.getIdOrdenCompra() != null) {
            codigoOC = ocRepo.findById(ins.getIdOrdenCompra()).map(OrdenCompra::getCodigo).orElse(null);
        }
        return Dtos.MaterialInstanciaDto.builder()
                .idInstancia(ins.getIdInstancia())
                .numeroSerie(ins.getNumeroSerie())
                .idMaterial(ins.getIdMaterial())
                .codigoMaterial(m != null ? m.getCodigo() : null)
                .nombreMaterial(m != null ? m.getNombre() : null)
                .modelo(m != null ? m.getModelo() : null)
                .marca(m != null ? m.getMarca() : null)
                .categoria(m != null ? m.getCategoria() : null)
                .idOrdenCompra(ins.getIdOrdenCompra())
                .codigoOC(codigoOC)
                .fechaIngreso(ins.getFechaIngreso() != null ? ins.getFechaIngreso().toString() : null)
                .estado(ins.getEstado())
                .build();
    }

    // ===== Marcado de instancias al salir/mermar stock (FIFO, best-effort) =====
    // No lanza excepción: si no hay suficientes instancias (p. ej. stock heredado
    // sin series), marca las que existan; el stock_actual ya se actualizó aparte.
    @Transactional
    public void marcarSalidaInstancias(Long idMaterial, int cantidad) {
        marcarInstancias(idMaterial, cantidad, "EN_ALMACEN", "SALIDA");
    }

    @Transactional
    public void marcarMermaInstancias(Long idMaterial, int cantidad) {
        marcarInstancias(idMaterial, cantidad, "EN_ALMACEN", "MERMADO");
    }

    private void marcarInstancias(Long idMaterial, int cantidad, String desdeEstado, String haciaEstado) {
        if (cantidad <= 0 || idMaterial == null) return;
        try {
            List<MaterialInstancia> enAlmacen = instanciaRepo.findByIdMaterialAndEstadoOrderByIdInstanciaAsc(
                    idMaterial, desdeEstado, org.springframework.data.domain.PageRequest.of(0, cantidad));
            for (MaterialInstancia ins : enAlmacen) {
                ins.setEstado(haciaEstado);
                instanciaRepo.save(ins);
            }
        } catch (Exception ignored) {
            // best-effort: el stock_actual ya quedó consistente
        }
    }

    // ===== RF08 - listar órdenes de compra abiertas =====
    @Transactional(readOnly = true)
    public List<Dtos.OrdenCompraDto> listarOrdenes(boolean incluirCerradas) {
        List<OrdenCompra> ordenes = incluirCerradas ? ocRepo.findAll() : ocRepo.findByEstado("ABIERTA");
        return ordenes.stream()
                .map(this::toOrdenDto)
                .toList();
    }

    @Transactional
    public Dtos.OrdenCompraDto crearOrden(Dtos.OrdenCompraCreate req) {
        if (ocRepo.findByCodigo(req.getCodigo()).isPresent()) {
            throw new ReglaNegocioException("El código de orden ya está registrado.");
        }
        String proveedor = req.getProveedor();
        Long idEmpresa = req.getIdEmpresa();
        if (idEmpresa != null) {
            Empresa emp = empresaRepo.findById(idEmpresa).orElseThrow(() ->
                    new ReglaNegocioException("La empresa (proveedor) seleccionada no existe."));
            if (!"PROVEEDOR".equalsIgnoreCase(emp.getTipo())) {
                throw new ReglaNegocioException("La empresa seleccionada no es un proveedor.");
            }
            proveedor = emp.getRazonSocial();
        } else if (proveedor == null || proveedor.isBlank()) {
            throw new ReglaNegocioException("Debe seleccionar un proveedor registrado o ingresar su nombre.");
        }
        OrdenCompra orden = OrdenCompra.builder()
                .codigo(req.getCodigo())
                .proveedor(proveedor)
                .idEmpresa(idEmpresa)
                .fechaEmision(req.getFechaEmision())
                .estado("ABIERTA")
                .build();
        orden = ocRepo.save(orden);
        guardarItems(orden.getIdOrdenCompra(), req.getItems());
        return toOrdenDto(orden);
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
        String proveedor = req.getProveedor();
        Long idEmpresa = req.getIdEmpresa();
        if (idEmpresa != null) {
            Empresa emp = empresaRepo.findById(idEmpresa).orElseThrow(() ->
                    new ReglaNegocioException("La empresa (proveedor) seleccionada no existe."));
            if (!"PROVEEDOR".equalsIgnoreCase(emp.getTipo())) {
                throw new ReglaNegocioException("La empresa seleccionada no es un proveedor.");
            }
            proveedor = emp.getRazonSocial();
        }
        orden.setCodigo(req.getCodigo());
        if (proveedor != null && !proveedor.isBlank()) {
            orden.setProveedor(proveedor);
        }
        orden.setIdEmpresa(idEmpresa);
        orden.setFechaEmision(req.getFechaEmision());
        orden = ocRepo.save(orden);
        if (req.getItems() != null) {
            detalleOcRepo.deleteByIdOrdenCompra(orden.getIdOrdenCompra());
            guardarItems(orden.getIdOrdenCompra(), req.getItems());
        }
        return toOrdenDto(orden);
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
        String ruc = null;
        if (orden.getIdEmpresa() != null) {
            ruc = empresaRepo.findById(orden.getIdEmpresa())
                    .map(Empresa::getRuc).orElse(null);
        }
        List<Dtos.DetalleOrdenCompraDto> items = detalleOcRepo
                .findByIdOrdenCompra(orden.getIdOrdenCompra()).stream()
                .map(this::toDetalleDto).toList();
        return Dtos.OrdenCompraDto.builder()
                .idOrdenCompra(orden.getIdOrdenCompra())
                .codigo(orden.getCodigo())
                .proveedor(orden.getProveedor())
                .idEmpresa(orden.getIdEmpresa())
                .rucProveedor(ruc)
                .fechaEmision(orden.getFechaEmision())
                .estado(orden.getEstado())
                .items(items)
                .build();
    }

    private Dtos.DetalleOrdenCompraDto toDetalleDto(DetalleOrdenCompra d) {
        String codigo = null, nombre = null, categoria = null, unidad = null;
        if (d.getIdMaterial() != null) {
            Material mat = materialRepo.findById(d.getIdMaterial()).orElse(null);
            if (mat != null) {
                codigo = mat.getCodigo();
                nombre = mat.getNombre();
                categoria = mat.getCategoria();
                unidad = mat.getUnidadMedida();
            }
        }
        return Dtos.DetalleOrdenCompraDto.builder()
                .idDetalleOc(d.getIdDetalleOc())
                .idMaterial(d.getIdMaterial())
                .cantidad(d.getCantidad())
                .precioUnitario(d.getPrecioUnitario())
                .codigoMaterial(codigo)
                .nombreMaterial(nombre)
                .categoria(categoria)
                .unidadMedida(unidad)
                .build();
    }

    private void guardarItems(Long idOrdenCompra, List<Dtos.DetalleOrdenCompraDto> items) {
        if (items == null || items.isEmpty()) return;
        for (Dtos.DetalleOrdenCompraDto item : items) {
            if (item.getIdMaterial() == null) continue;
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new ReglaNegocioException("La cantidad de cada ítem debe ser mayor que cero.");
            }
            if (!materialRepo.existsById(item.getIdMaterial())) {
                throw new ReglaNegocioException("El material del ítem no existe (id=" + item.getIdMaterial() + ").");
            }
            DetalleOrdenCompra det = DetalleOrdenCompra.builder()
                    .idOrdenCompra(idOrdenCompra)
                    .idMaterial(item.getIdMaterial())
                    .cantidad(item.getCantidad())
                    .precioUnitario(item.getPrecioUnitario())
                    .build();
            detalleOcRepo.save(det);
        }
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

    // ===== Stock agrupado por categoria =====
    @Transactional(readOnly = true)
    public List<Dtos.CategoriaStockDto> stockPorCategoria() {
        java.util.Map<String, List<Material>> porCat = materialRepo.findAll().stream()
                .filter(m -> "ACTIVO".equals(m.getEstado()))
                .collect(java.util.stream.Collectors.groupingBy(
                        m -> (m.getCategoria() == null || m.getCategoria().isBlank())
                                ? "Sin categoría" : m.getCategoria()));
        return porCat.entrySet().stream()
                .map(e -> {
                    List<Material> lista = e.getValue();
                    int stockTotal = lista.stream().mapToInt(Material::getStockActual).sum();
                    int stockMinTotal = lista.stream().mapToInt(Material::getStockMinimo).sum();
                    java.math.BigDecimal valor = lista.stream()
                            .map(m -> m.getPrecioUnitario().multiply(java.math.BigDecimal.valueOf(m.getStockActual())))
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                    long alertas = lista.stream().filter(m -> m.getStockActual() <= m.getStockMinimo()).count();
                    return Dtos.CategoriaStockDto.builder()
                            .categoria(e.getKey())
                            .numMateriales(lista.size())
                            .stockTotal(stockTotal)
                            .stockMinimoTotal(stockMinTotal)
                            .valorInventario(valor)
                            .enAlerta(alertas)
                            .build();
                })
                .sorted(java.util.Comparator.comparing(Dtos.CategoriaStockDto::getCategoria))
                .toList();
    }

    // ===== Categorias disponibles (para dropdowns de filtros) =====
    @Transactional(readOnly = true)
    public List<String> categoriasDisponibles() {
        return materialRepo.findAll().stream()
                .map(Material::getCategoria)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    // ===== Código automático según categoría =====
    // Genera un código tipo <PREFIJO>-<NNNN> donde el prefijo son las primeras
    // 4 letras de la categoría (sin acentos, mayúsculas) y NNNN es el siguiente
    // consecutivo entre los materiales cuyo código empieza con ese prefijo.
    // Ej.: PROTECCION -> PROT-0001, INTERRUPCION -> INTE-0001, CABLE -> CABL-0001
    @Transactional(readOnly = true)
    public String siguienteCodigo(String categoria) {
        String prefijo = prefijoCategoria(categoria);
        int maxSeq = materialRepo.findAll().stream()
                .map(Material::getCodigo)
                .filter(c -> c != null && c.startsWith(prefijo + "-"))
                .mapToInt(c -> {
                    try {
                        String num = c.substring(prefijo.length() + 1).replaceAll("[^0-9]", "");
                        return num.isEmpty() ? 0 : Integer.parseInt(num);
                    } catch (Exception e) { return 0; }
                })
                .max()
                .orElse(0);
        return String.format("%s-%04d", prefijo, maxSeq + 1);
    }

    private String prefijoCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) return "MAT";
        String limpio = java.text.Normalizer.normalize(categoria, java.text.Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9]", "");
        if (limpio.isEmpty()) return "MAT";
        return limpio.length() >= 4 ? limpio.substring(0, 4)
                : String.format("%-4s", limpio).replace(' ', '0');
    }

    private boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase().contains(texto.toLowerCase());
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
