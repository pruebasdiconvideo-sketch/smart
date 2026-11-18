package com.smartpoweron.gestion_materiales.service;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.entity.ParametroPrueba;
import com.smartpoweron.gestion_materiales.entity.Proyecto;
import com.smartpoweron.gestion_materiales.entity.PruebaCalidad;
import com.smartpoweron.gestion_materiales.entity.Usuario;
import com.smartpoweron.gestion_materiales.exception.ReglaNegocioException;
import com.smartpoweron.gestion_materiales.repository.ParametroPruebaRepository;
import com.smartpoweron.gestion_materiales.repository.ProyectoRepository;
import com.smartpoweron.gestion_materiales.repository.PruebaCalidadRepository;
import com.smartpoweron.gestion_materiales.repository.UsuarioRepository;
import com.smartpoweron.gestion_materiales.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de calidad (RF25 a RF30, §7.2 - M4).
 *
 * Reglas:
 *   P4-RN03 / R-CAL-01 / RF26: el resultado CONFORME/NO_CONFORME lo calcula el
 *       servidor comparando valor_medido con el rango del parámetro; el
 *       inspector no lo digita. Sólo el ADMIN cambia los rangos (RF30), y los
 *       cambios no alteran pruebas ya registradas.
 *   P4-RN02 / RF27: si NO_CONFORME, se registra la observación automáticamente.
 *   P4-RN01 / R-CAL-02 / RF28: el certificado PDF sólo se genera si la ÚLTIMA
 *       medición de cada parámetro del proyecto es CONFORME.
 */
@Service
public class CalidadService {

    private final PruebaCalidadRepository pruebaRepo;
    private final ParametroPruebaRepository parametroRepo;
    private final ProyectoRepository proyectoRepo;
    private final UsuarioRepository usuarioRepo;
    private final SecurityUtil securityUtil;

    public CalidadService(PruebaCalidadRepository pruebaRepo,
                          ParametroPruebaRepository parametroRepo,
                          ProyectoRepository proyectoRepo,
                          UsuarioRepository usuarioRepo,
                          SecurityUtil securityUtil) {
        this.pruebaRepo = pruebaRepo;
        this.parametroRepo = parametroRepo;
        this.proyectoRepo = proyectoRepo;
        this.usuarioRepo = usuarioRepo;
        this.securityUtil = securityUtil;
    }

    // ===== RF25, RF26, RF27 - registrar prueba =====
    @Transactional
    public Dtos.PruebaCalidadDto registrarPrueba(Dtos.PruebaCalidadCreate req) {
        Usuario inspector = securityUtil.usuarioOExcepcion();
        Proyecto p = proyectoRepo.findById(req.getIdProyecto()).orElseThrow(() ->
                new ReglaNegocioException("El proyecto no existe."));
        if ("ANULADO".equals(p.getEstado())) {
            throw new ReglaNegocioException("No se pueden registrar pruebas para un proyecto anulado.");
        }
        ParametroPrueba param = parametroRepo.findById(req.getIdParametro()).orElseThrow(() ->
                new ReglaNegocioException("El parámetro de prueba no existe."));
        if (!Boolean.TRUE.equals(param.getActivo())) {
            throw new ReglaNegocioException("El parámetro de prueba está inactivo.");
        }
        // P4-RN03: el resultado lo calcula el sistema
        String resultado = calcularResultado(param, req.getValorMedido());
        String observacion = req.getObservacion();
        if ("NO_CONFORME".equals(resultado)) {
            // P4-RN02: registrar observación automática si el inspector no la mandó
            if (observacion == null || observacion.isBlank()) {
                observacion = "Valor " + req.getValorMedido() + " " + param.getUnidad()
                        + " fuera del rango [" + param.getValorMinimo() + " - " + param.getValorMaximo() + "].";
            }
        }
        PruebaCalidad pc = PruebaCalidad.builder()
                .idProyecto(p.getIdProyecto())
                .idParametro(param.getIdParametro())
                .idUsuarioInspector(inspector.getIdUsuario())
                .valorMedido(req.getValorMedido())
                .resultado(resultado)
                .observacion(observacion)
                .tecnicoExterno(req.getTecnicoExterno())
                .fechaPrueba(LocalDateTime.now())
                .build();
        return toDto(pruebaRepo.save(pc));
    }

    @Transactional
    public Dtos.PruebaCalidadDto actualizarPrueba(Long id, Dtos.PruebaCalidadCreate req) {
        PruebaCalidad anterior = pruebaRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La prueba de calidad no existe."));
        if (Boolean.TRUE.equals(anterior.getAnulada())) {
            throw new ReglaNegocioException("La prueba ya está anulada.");
        }
        Dtos.PruebaCalidadDto reemplazo = registrarPrueba(req);
        anterior.setAnulada(true);
        anterior.setMotivoAnulacion("Reemplazada por la prueba #" + reemplazo.getIdPrueba());
        pruebaRepo.save(anterior);
        return reemplazo;
    }

    @Transactional
    public void anularPrueba(Long id, String motivo) {
        PruebaCalidad prueba = pruebaRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La prueba de calidad no existe."));
        if (Boolean.TRUE.equals(prueba.getAnulada())) {
            throw new ReglaNegocioException("La prueba ya está anulada.");
        }
        prueba.setAnulada(true);
        prueba.setMotivoAnulacion(motivo.trim());
        pruebaRepo.save(prueba);
    }

    /** P4-RN03: el resultado se calcula con el rango técnico. */
    private String calcularResultado(ParametroPrueba param, BigDecimal valor) {
        if (valor == null) return "NO_CONFORME";
        if (param.getValorMinimo() != null && valor.compareTo(param.getValorMinimo()) < 0) return "NO_CONFORME";
        if (param.getValorMaximo() != null && valor.compareTo(param.getValorMaximo()) > 0) return "NO_CONFORME";
        return "CONFORME";
    }

    // ===== RF27, RF29 - historial por proyecto =====
    @Transactional(readOnly = true)
    public List<Dtos.PruebaCalidadDto> listarPorProyecto(Long idProyecto) {
        return pruebaRepo.findByIdProyectoOrderByIdPruebaDesc(idProyecto).stream()
                .map(this::toDto).toList();
    }

    // ===== RF30 - listar / configurar rangos =====
    @Transactional(readOnly = true)
    public List<Dtos.ParametroPruebaDto> listarParametros(boolean incluirInactivos) {
        return parametroRepo.findAll().stream()
                .filter(p -> incluirInactivos || Boolean.TRUE.equals(p.getActivo()))
                .map(this::toParametroDto).toList();
    }

    @Transactional
    public Dtos.ParametroPruebaDto crearParametro(Dtos.ParametroCreate req) {
        validarRango(req.getValorMinimo(), req.getValorMaximo());
        ParametroPrueba param = ParametroPrueba.builder()
                .tipoPrueba(req.getTipoPrueba())
                .unidad(req.getUnidad())
                .valorMinimo(req.getValorMinimo())
                .valorMaximo(req.getValorMaximo())
                .normaReferencia(req.getNormaReferencia())
                .activo(true)
                .build();
        return toParametroDto(parametroRepo.save(param));
    }

    @Transactional
    public Dtos.ParametroPruebaDto configurarRango(Long id, Dtos.ParametroUpdate req) {
        ParametroPrueba param = parametroRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("El parámetro no existe."));
        if (!Boolean.TRUE.equals(param.getActivo())) {
            throw new ReglaNegocioException("No se puede editar un parámetro inactivo.");
        }
        validarRango(req.getValorMinimo(), req.getValorMaximo());
        param.setTipoPrueba(req.getTipoPrueba());
        param.setUnidad(req.getUnidad());
        param.setValorMinimo(req.getValorMinimo());
        param.setValorMaximo(req.getValorMaximo());
        param.setNormaReferencia(req.getNormaReferencia());
        // Los cambios no alteran pruebas ya registradas: no se hace update cascada.
        return toParametroDto(parametroRepo.save(param));
    }

    @Transactional
    public void desactivarParametro(Long id) {
        ParametroPrueba param = parametroRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("El parámetro no existe."));
        if (!Boolean.TRUE.equals(param.getActivo())) {
            throw new ReglaNegocioException("El parámetro ya está inactivo.");
        }
        param.setActivo(false);
        parametroRepo.save(param);
    }

    @Transactional
    public void reactivarParametro(Long id) {
        ParametroPrueba param = parametroRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("El parámetro no existe."));
        if (Boolean.TRUE.equals(param.getActivo())) {
            throw new ReglaNegocioException("El parámetro ya está activo.");
        }
        param.setActivo(true);
        parametroRepo.save(param);
    }

    // ===== RF28 - verificar si el proyecto puede certificar =====
    @Transactional(readOnly = true)
    public boolean puedeCertificar(Long idProyecto) {
        proyectoRepo.findById(idProyecto).orElseThrow(() ->
                new ReglaNegocioException("El proyecto no existe."));
        // Para cada parámetro, la última prueba debe ser CONFORME.
        List<ParametroPrueba> params = parametroRepo.findAll().stream()
                .filter(pp -> Boolean.TRUE.equals(pp.getActivo()))
                .toList();
        if (params.isEmpty()) {
            return false;
        }
        for (ParametroPrueba pp : params) {
            List<PruebaCalidad> pruebas = pruebaRepo
                    .findByIdProyectoOrderByIdPruebaDesc(idProyecto).stream()
                    .filter(pc -> !Boolean.TRUE.equals(pc.getAnulada()))
                    .filter(pc -> pc.getIdParametro().equals(pp.getIdParametro()))
                    .toList();
            if (pruebas.isEmpty()) {
                // Si no hay mediciones para este parámetro, no se certifica todavía.
                // (un proyecto con parámetros pendientes no se certifica.)
                return false;
            }
            if (!"CONFORME".equals(pruebas.get(0).getResultado())) {
                return false; // última medición del parámetro no es conforme
            }
        }
        return true;
    }

    // ===== Mappers =====
    @Transactional(readOnly = true)
    public Dtos.PruebaCalidadDto toDto(PruebaCalidad pc) {
        Proyecto p = proyectoRepo.findById(pc.getIdProyecto()).orElse(null);
        ParametroPrueba param = parametroRepo.findById(pc.getIdParametro()).orElse(null);
        Usuario u = usuarioRepo.findById(pc.getIdUsuarioInspector()).orElse(null);
        return Dtos.PruebaCalidadDto.builder()
                .idPrueba(pc.getIdPrueba())
                .idProyecto(pc.getIdProyecto())
                .idParametro(pc.getIdParametro())
                .codigoProyecto(p == null ? null : p.getCodigo())
                .tipoPrueba(param == null ? null : param.getTipoPrueba())
                .unidad(param == null ? null : param.getUnidad())
                .valorMinimo(param == null ? null : param.getValorMinimo())
                .valorMaximo(param == null ? null : param.getValorMaximo())
                .valorMedido(pc.getValorMedido())
                .resultado(pc.getResultado())
                .observacion(pc.getObservacion())
                .inspector(u == null ? null : u.getNombreCompleto())
                .tecnicoExterno(pc.getTecnicoExterno())
                .fechaPrueba(pc.getFechaPrueba())
                .anulada(pc.getAnulada())
                .motivoAnulacion(pc.getMotivoAnulacion())
                .build();
    }

    private Dtos.ParametroPruebaDto toParametroDto(ParametroPrueba p) {
        return Dtos.ParametroPruebaDto.builder()
                .idParametro(p.getIdParametro())
                .tipoPrueba(p.getTipoPrueba())
                .unidad(p.getUnidad())
                .valorMinimo(p.getValorMinimo())
                .valorMaximo(p.getValorMaximo())
                .normaReferencia(p.getNormaReferencia())
                .activo(p.getActivo())
                .build();
    }

    private void validarRango(BigDecimal minimo, BigDecimal maximo) {
        if (minimo.compareTo(maximo) > 0) {
            throw new ReglaNegocioException("El valor mínimo no puede ser mayor que el máximo.");
        }
    }
}
