package com.smartpoweron.gestion_materiales.controller;

import com.smartpoweron.gestion_materiales.security.SecurityUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de vistas Thymeleaf (V01 a V16 - §7.4 / Tabla 69).
 * Cada vista consume los servicios REST desde el navegador (cookie JWT), por
 * lo que aquí sólo se renderiza la plantilla con datos de contexto (rol,
 * nombre del usuario). Las reglas de acceso por rol las impone SecurityConfig.
 */
@Controller
public class PageController {

    private final SecurityUtil securityUtil;

    public PageController(SecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
    }

    private void contexto(Model m) {
        securityUtil.usuarioActual().ifPresent(u -> {
            m.addAttribute("nombreUsuario", u.getNombreCompleto());
            m.addAttribute("correoUsuario", u.getCorreo());
        });
        securityUtil.rolActualOptional().ifPresent(r -> m.addAttribute("rolUsuario", r.getNombre()));
    }

    // ===== A0 =====
    // V01 /login (público)
    @GetMapping("/login")
    public String login() { return "auth/login"; }

    // V02 /inicio (todos)
    @GetMapping({"/", "/inicio"})
    public String inicio(Model m) {
        contexto(m);
        return "home";
    }

    // ===== M1 - Inventario =====
    // V03 /almacen/catalogo (ALMACENERO, ADMIN, JEFE_PRODUCCION)
    @GetMapping("/almacen/catalogo")
    public String catalogo(Model m) { contexto(m); return "almacen/catalogo"; }

    // V04 /almacen/ingresos (ALMACENERO)
    @GetMapping("/almacen/ingresos")
    public String ingresos(Model m) { contexto(m); return "almacen/ingresos"; }

    // ===== M2 - Producción =====
    // V05 /produccion/proyectos (JEFE_PRODUCCION, ADMIN)
    @GetMapping("/produccion/proyectos")
    public String proyectos(Model m) { contexto(m); return "produccion/proyectos"; }

    // V06 /produccion/solicitudes (JEFE_PRODUCCION, ADMIN consulta)
    @GetMapping("/produccion/solicitudes")
    public String solicitudes(Model m) { contexto(m); return "produccion/solicitudes"; }

    // V07 /produccion/aprobaciones (ADMIN)
    @GetMapping("/produccion/aprobaciones")
    public String aprobaciones(Model m) { contexto(m); return "produccion/aprobaciones"; }

    // ===== M3 - Despacho =====
    // V08 /despacho/bandeja (ALMACENERO)
    @GetMapping("/despacho/bandeja")
    public String bandeja(Model m) { contexto(m); return "despacho/bandeja"; }

    // V09 /despacho/preparar (ALMACENERO)
    @GetMapping("/despacho/preparar")
    public String preparar(Model m) { contexto(m); return "despacho/preparar"; }

    // V10 /despacho/confirmar (OPERARIO_ENSAMBLE)
    @GetMapping("/despacho/confirmar")
    public String confirmar(Model m) { contexto(m); return "despacho/confirmar"; }

    // ===== M4 - Calidad =====
    // V11 /calidad/pruebas (INSPECTOR, JEFE_PRODUCCION consulta, ADMIN consulta)
    @GetMapping("/calidad/pruebas")
    public String pruebas(Model m) { contexto(m); return "calidad/pruebas"; }

    // ===== M5 - Mermas =====
    // V12 /merma/devoluciones (OPERARIO, ALMACENERO, JEFE_PRODUCCION)
    @GetMapping("/merma/devoluciones")
    public String devoluciones(Model m) { contexto(m); return "merma/devoluciones"; }

    // V13 /merma/reporte (JEFE_PRODUCCION)
    @GetMapping("/merma/reporte")
    public String reporte(Model m) { contexto(m); return "merma/reporte"; }

    // ===== M6 - Trazabilidad =====
    // V14 /trazabilidad (JEFE_PRODUCCION, ADMIN)
    @GetMapping("/trazabilidad")
    public String trazabilidad(Model m) { contexto(m); return "trazabilidad/consulta"; }

    // ===== M0 - Administración =====
    // V15 /admin/usuarios (ADMIN)
    @GetMapping("/admin/usuarios")
    public String usuarios(Model m) { contexto(m); return "admin/usuarios"; }

    // V16 /admin/parametros (ADMIN)
    @GetMapping("/admin/parametros")
    public String parametros(Model m) { contexto(m); return "admin/parametros"; }
}
