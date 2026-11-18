package com.smartpoweron.gestion_materiales;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.repository.MaterialRepository;
import com.smartpoweron.gestion_materiales.repository.MermaDevolucionRepository;
import com.smartpoweron.gestion_materiales.repository.DetalleSolicitudRepository;
import com.smartpoweron.gestion_materiales.repository.DespachoRepository;
import com.smartpoweron.gestion_materiales.repository.OrdenCompraRepository;
import com.smartpoweron.gestion_materiales.repository.ParametroPruebaRepository;
import com.smartpoweron.gestion_materiales.repository.ProyectoRepository;
import com.smartpoweron.gestion_materiales.repository.PruebaCalidadRepository;
import com.smartpoweron.gestion_materiales.repository.TrazabilidadRepository;
import com.smartpoweron.gestion_materiales.service.CalidadService;
import com.smartpoweron.gestion_materiales.service.DespachoService;
import com.smartpoweron.gestion_materiales.service.MaterialService;
import com.smartpoweron.gestion_materiales.service.MermaService;
import com.smartpoweron.gestion_materiales.service.ProyectoService;
import com.smartpoweron.gestion_materiales.service.SolicitudService;
import com.smartpoweron.gestion_materiales.service.UsuarioService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
class GestionMaterialesApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MaterialRepository materialRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ProyectoService proyectoService;

	@Autowired
	private ProyectoRepository proyectoRepository;

	@Autowired
	private MaterialService materialService;

	@Autowired
	private OrdenCompraRepository ordenCompraRepository;

	@Autowired
	private CalidadService calidadService;

	@Autowired
	private MermaService mermaService;

	@Autowired
	private MermaDevolucionRepository mermaDevolucionRepository;

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private ParametroPruebaRepository parametroPruebaRepository;

	@Autowired
	private SolicitudService solicitudService;

	@Autowired
	private DespachoService despachoService;

	@Autowired
	private DetalleSolicitudRepository detalleSolicitudRepository;

	@Autowired
	private DespachoRepository despachoRepository;

	@Autowired
	private TrazabilidadRepository trazabilidadRepository;

	@Autowired
	private PruebaCalidadRepository pruebaCalidadRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void rootRouteRendersHomePage() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("home"));
	}

	@Test
	void materialRepositoryReadsAllMappedColumns() {
		assertNotNull(materialRepository.findAll());
	}

	@Test
	void projectCreateAcceptsIsoDate() throws Exception {
		Dtos.ProyectoCreate request = objectMapper.readValue(
				"{\"codigo\":\"TST-001\",\"cliente\":\"Cliente\",\"fechaInicio\":\"2012-11-11\",\"presupuesto\":1000}",
				Dtos.ProyectoCreate.class);

		assertEquals(LocalDate.of(2012, 11, 11), request.getFechaInicio());
	}

	@Test
	void projectCanBeUpdatedAndSoftCancelled() {
		String codigo = "TST-" + java.util.UUID.randomUUID();
		Dtos.ProyectoCreate create = Dtos.ProyectoCreate.builder()
				.codigo(codigo)
				.cliente("Cliente inicial")
				.fechaInicio(LocalDate.of(2026, 11, 17))
				.presupuesto(new java.math.BigDecimal("1000"))
				.build();
		Dtos.ProyectoDto created = proyectoService.crearProyecto(create);

		Dtos.ProyectoCreate update = Dtos.ProyectoCreate.builder()
				.codigo(codigo + "-EDIT")
				.cliente("Cliente actualizado")
				.fechaInicio(LocalDate.of(2026, 11, 18))
				.presupuesto(new java.math.BigDecimal("1200"))
				.build();
		assertEquals("Cliente actualizado",
				proyectoService.actualizarProyecto(created.getIdProyecto(), update).getCliente());

		proyectoService.anularProyecto(created.getIdProyecto());
		assertEquals("ANULADO", proyectoRepository.findById(created.getIdProyecto()).orElseThrow().getEstado());
		assertTrue(proyectoRepository.existsById(created.getIdProyecto()));
	}

	@Test
	void materialCanBeUpdatedAndSoftDeactivated() {
		String codigo = "MAT-" + java.util.UUID.randomUUID();
		Dtos.MaterialCreate create = Dtos.MaterialCreate.builder()
				.codigo(codigo)
				.nombre("Material de prueba")
				.precioUnitario(new java.math.BigDecimal("12.50"))
				.stockMinimo(2)
				.build();
		Dtos.MaterialDto created = materialService.registrarMaterial(create);

		Dtos.MaterialUpdate update = Dtos.MaterialUpdate.builder()
				.codigo(codigo)
				.nombre("Material actualizado")
				.precioUnitario(new java.math.BigDecimal("15.00"))
				.stockMinimo(3)
				.build();
		assertEquals("Material actualizado",
				materialService.actualizarMaterial(created.getIdMaterial(), update).getNombre());

		materialService.desactivarMaterial(created.getIdMaterial());
		assertTrue(materialService.consultarCatalogo(codigo, null, null, false).isEmpty());
		assertEquals("INACTIVO", materialService.consultarCatalogo(codigo, null, null, true)
				.get(0).getEstado());
		materialService.reactivarMaterial(created.getIdMaterial());
		assertEquals("ACTIVO", materialService.consultarCatalogo(codigo, null, null, false)
				.get(0).getEstado());
	}

	@Test
	void userCanBeEditedDeactivatedAndReactivated() {
		String correo = "crud-" + java.util.UUID.randomUUID() + "@smart.pe";
		Dtos.UsuarioDto created = usuarioService.crear(Dtos.UsuarioCreate.builder()
				.idRol(4L)
				.nombreCompleto("Usuario de prueba")
				.correo(correo)
				.password("Prueba123")
				.build());
		Dtos.UsuarioDto updated = usuarioService.actualizar(created.getIdUsuario(),
				Dtos.UsuarioUpdate.builder()
						.idRol(5L)
						.nombreCompleto("Usuario actualizado")
						.correo(correo)
						.password("NuevaPrueba123")
						.build());
		assertEquals("Usuario actualizado", updated.getNombreCompleto());
		assertEquals("INSPECTOR", updated.getRol());

		usuarioService.desactivar(created.getIdUsuario());
		assertEquals(Boolean.FALSE, usuarioService.listar().stream()
				.filter(u -> u.getIdUsuario().equals(created.getIdUsuario()))
				.findFirst().orElseThrow().getActivo());
		usuarioService.reactivar(created.getIdUsuario());
		assertEquals(Boolean.TRUE, usuarioService.listar().stream()
				.filter(u -> u.getIdUsuario().equals(created.getIdUsuario()))
				.findFirst().orElseThrow().getActivo());
	}

	@Test
	void purchaseOrderCanBeEditedClosedAndReopened() {
		String codigo = "OC-" + java.util.UUID.randomUUID();
		Dtos.OrdenCompraDto created = materialService.crearOrden(Dtos.OrdenCompraCreate.builder()
				.codigo(codigo)
				.proveedor("Proveedor inicial")
				.fechaEmision(LocalDate.of(2026, 11, 17))
				.build());
		Dtos.OrdenCompraDto updated = materialService.actualizarOrden(created.getIdOrdenCompra(),
				Dtos.OrdenCompraUpdate.builder()
						.codigo(codigo)
						.proveedor("Proveedor actualizado")
						.fechaEmision(LocalDate.of(2026, 11, 18))
						.build());
		assertEquals("Proveedor actualizado", updated.getProveedor());
		materialService.cerrarOrden(created.getIdOrdenCompra());
		assertEquals("CERRADA", ordenCompraRepository.findById(created.getIdOrdenCompra())
				.orElseThrow().getEstado());
		materialService.reabrirOrden(created.getIdOrdenCompra());
		assertEquals("ABIERTA", ordenCompraRepository.findById(created.getIdOrdenCompra())
				.orElseThrow().getEstado());
	}

	@Test
	void testParameterSupportsLogicalDeactivation() {
		Dtos.ParametroPruebaDto created = calidadService.crearParametro(Dtos.ParametroCreate.builder()
				.tipoPrueba("PRUEBA-" + java.util.UUID.randomUUID())
				.unidad("V")
				.valorMinimo(new java.math.BigDecimal("1"))
				.valorMaximo(new java.math.BigDecimal("10"))
				.build());
		calidadService.configurarRango(created.getIdParametro(), Dtos.ParametroUpdate.builder()
				.tipoPrueba(created.getTipoPrueba())
				.unidad("mV")
				.valorMinimo(new java.math.BigDecimal("2"))
				.valorMaximo(new java.math.BigDecimal("20"))
				.build());
		assertEquals("mV", parametroPruebaRepository.findById(created.getIdParametro())
				.orElseThrow().getUnidad());
		calidadService.desactivarParametro(created.getIdParametro());
		assertTrue(calidadService.listarParametros(false).stream()
				.noneMatch(p -> p.getIdParametro().equals(created.getIdParametro())));
		assertEquals(Boolean.FALSE, calidadService.listarParametros(true).stream()
				.filter(p -> p.getIdParametro().equals(created.getIdParametro()))
				.findFirst().orElseThrow().getActivo());
		calidadService.reactivarParametro(created.getIdParametro());
		assertEquals(Boolean.TRUE, parametroPruebaRepository.findById(created.getIdParametro())
				.orElseThrow().getActivo());
	}

	@Test
	void cancellingConfirmedRequestCompensatesStockAndKeepsTrace() {
		int stockInicial = materialRepository.findById(1L).orElseThrow().getStockActual();
		try {
			autenticarComo("jefe@smart.pe", "JEFE_PRODUCCION");
			Dtos.SolicitudDto solicitud = solicitudService.registrarConBom(Dtos.SolicitudCreate.builder()
					.idProyecto(1L)
					.bom(java.util.List.of(Dtos.DetalleSolicitudCreate.builder()
							.idMaterial(1L).cantidad(1).build()))
					.build());
			Long idDetalle = detalleSolicitudRepository.findByIdSolicitud(solicitud.getIdSolicitud())
					.get(0).getIdDetalleSolicitud();

			autenticarComo("almacen@smart.pe", "ALMACENERO");
			Dtos.DespachoDto despacho = despachoService.registrarDespacho(Dtos.DespachoRequest.builder()
					.idSolicitud(solicitud.getIdSolicitud())
					.items(java.util.List.of(Dtos.DespachoItemRequest.builder()
							.idDetalleSolicitud(idDetalle)
							.codigoDigitado("SIPROTEC-7SJ62")
							.cantidadEntregada(1)
							.build()))
					.build());

			autenticarComo("operario@smart.pe", "OPERARIO_ENSAMBLE");
			despachoService.confirmarEntrega(despacho.getIdDespacho());
			assertEquals(stockInicial - 1, materialRepository.findById(1L).orElseThrow().getStockActual());

			autenticarComo("jefe@smart.pe", "JEFE_PRODUCCION");
			Dtos.SolicitudDto anulada = solicitudService.anular(solicitud.getIdSolicitud(),
					Dtos.SolicitudAnulacionRequest.builder().motivo("Prueba de compensación").build());

			assertTrue(Boolean.TRUE.equals(anulada.getAnulada()));
			assertEquals(stockInicial, materialRepository.findById(1L).orElseThrow().getStockActual());
			assertTrue(Boolean.TRUE.equals(despachoRepository.findById(despacho.getIdDespacho())
					.orElseThrow().getAnulado()));
			assertEquals("DEVOLUCION", trazabilidadRepository
					.findByIdMaterialOrderByIdTrazabilidadDesc(1L).get(0).getTipoMovimiento());
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void pendingRequestAndDispatchCanBeEditedAndSoftCancelled() {
		int stockInicial = materialRepository.findById(1L).orElseThrow().getStockActual();
		try {
			autenticarComo("jefe@smart.pe", "JEFE_PRODUCCION");
			Dtos.SolicitudDto solicitud = solicitudService.registrarConBom(Dtos.SolicitudCreate.builder()
					.idProyecto(1L)
					.bom(java.util.List.of(Dtos.DetalleSolicitudCreate.builder()
							.idMaterial(1L).cantidad(1).build()))
					.build());
			Dtos.SolicitudDto editada = solicitudService.actualizar(solicitud.getIdSolicitud(),
					Dtos.SolicitudCreate.builder()
							.idProyecto(1L)
							.bom(java.util.List.of(Dtos.DetalleSolicitudCreate.builder()
									.idMaterial(1L).cantidad(2).build()))
							.build());
			assertEquals(2, editada.getDetalle().get(0).getCantidadSolicitada());

			autenticarComo("almacen@smart.pe", "ALMACENERO");
			Long idDetalle = editada.getDetalle().get(0).getIdDetalleSolicitud();
			Dtos.DespachoDto despacho = despachoService.registrarDespacho(Dtos.DespachoRequest.builder()
					.idSolicitud(solicitud.getIdSolicitud())
					.items(java.util.List.of(Dtos.DespachoItemRequest.builder()
							.idDetalleSolicitud(idDetalle)
							.codigoDigitado("SIPROTEC-7SJ62")
							.cantidadEntregada(1)
							.build()))
					.build());
			Dtos.DespachoDto despachoEditado = despachoService.actualizarDespacho(despacho.getIdDespacho(),
					Dtos.DespachoRequest.builder()
							.idSolicitud(solicitud.getIdSolicitud())
							.items(java.util.List.of(Dtos.DespachoItemRequest.builder()
									.idDetalleSolicitud(idDetalle)
									.codigoDigitado("SIPROTEC-7SJ62")
									.cantidadEntregada(2)
									.build()))
							.build());
			assertEquals("COMPLETO", despachoEditado.getTipo());
			despachoService.anular(despacho.getIdDespacho(), "Corrección previa a confirmar");
			assertEquals(stockInicial, materialRepository.findById(1L).orElseThrow().getStockActual());

			autenticarComo("jefe@smart.pe", "JEFE_PRODUCCION");
			solicitudService.anular(solicitud.getIdSolicitud(),
					Dtos.SolicitudAnulacionRequest.builder().motivo("Solicitud cancelada").build());
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void updatingQualityTestPreservesSupersededRecordAndSupportsLogicalDelete() {
		try {
			autenticarComo("inspector@smart.pe", "INSPECTOR");
			Dtos.PruebaCalidadCreate body = Dtos.PruebaCalidadCreate.builder()
					.idProyecto(1L)
					.idParametro(1L)
					.valorMedido(new java.math.BigDecimal("400"))
					.observacion("Medición inicial")
					.build();
			Dtos.PruebaCalidadDto original = calidadService.registrarPrueba(body);
			Dtos.PruebaCalidadDto reemplazo = calidadService.actualizarPrueba(original.getIdPrueba(),
					Dtos.PruebaCalidadCreate.builder()
							.idProyecto(1L)
							.idParametro(1L)
							.valorMedido(new java.math.BigDecimal("401"))
							.observacion("Medición corregida")
							.build());

			assertTrue(Boolean.TRUE.equals(pruebaCalidadRepository.findById(original.getIdPrueba())
					.orElseThrow().getAnulada()));
			assertEquals(0, reemplazo.getValorMedido().compareTo(new java.math.BigDecimal("401")));

			calidadService.anularPrueba(reemplazo.getIdPrueba(), "Registro duplicado");
			assertTrue(Boolean.TRUE.equals(pruebaCalidadRepository.findById(reemplazo.getIdPrueba())
					.orElseThrow().getAnulada()));
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void cancellingReintegratedReturnReversesStockAndAddsCompensatingTrace() {
		int stockInicial = materialRepository.findById(1L).orElseThrow().getStockActual();
		try {
			autenticarComo("operario@smart.pe", "OPERARIO_ENSAMBLE");
			Dtos.MermaDevolucionDto devolucion = mermaService.registrarDevolucion(
					Dtos.DevolucionCreate.builder().idProyecto(1L).idMaterial(1L).cantidad(1).build());
			autenticarComo("almacen@smart.pe", "ALMACENERO");
			mermaService.reintegrar(devolucion.getIdMerma());
			assertEquals(stockInicial + 1, materialRepository.findById(1L).orElseThrow().getStockActual());

			mermaService.anular(devolucion.getIdMerma(), "Prueba de compensación");
			assertEquals(stockInicial, materialRepository.findById(1L).orElseThrow().getStockActual());
			assertTrue(Boolean.TRUE.equals(mermaDevolucionRepository.findById(devolucion.getIdMerma())
					.orElseThrow().getAnulada()));
			assertEquals("MERMA", trazabilidadRepository
					.findByIdMaterialOrderByIdTrazabilidadDesc(1L).get(0).getTipoMovimiento());
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	private void autenticarComo(String correo, String rol) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				correo, "test", AuthorityUtils.createAuthorityList("ROLE_" + rol)));
	}

}
