package com.smartpoweron.gestion_materiales.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Atributos comunes a todas las vistas Thymeleaf:
 *   - {@code currentPath}: ruta actual para que el menú lateral marque el ítem activo.
 *   - {@code currentMenu}: primer segmento de la ruta (almacen, produccion, ...)
 *     para agrupar resaltado por módulo.
 *   - {@code initials}: iniciales del usuario activo para el avatar del topbar.
 *
 * Se aplica antes que cada controlador renderice su vista, sin importar si
 * éste invoca o no a {@code contexto(m)}.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute
    public void commonAttributes(HttpServletRequest request, Model model) {
        String path = request.getRequestURI();
        model.addAttribute("currentPath", path);

        // Primer segmento significativo: /almacen/catalogo -> almacen
        String menu = "";
        if (path != null && path.length() > 1) {
            String trimmed = path.startsWith("/") ? path.substring(1) : path;
            int slash = trimmed.indexOf('/');
            menu = slash > 0 ? trimmed.substring(0, slash) : trimmed;
        }
        model.addAttribute("currentMenu", menu);
    }
}
