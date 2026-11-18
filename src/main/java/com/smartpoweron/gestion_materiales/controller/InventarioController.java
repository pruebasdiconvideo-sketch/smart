package com.smartpoweron.gestion_materiales.controller;

import com.smartpoweron.gestion_materiales.dto.IngresoMaterialDTO;
import com.smartpoweron.gestion_materiales.dto.ReposicionMaterialDTO;
import com.smartpoweron.gestion_materiales.entity.Material;
import com.smartpoweron.gestion_materiales.entity.OrdenCompra;
import com.smartpoweron.gestion_materiales.entity.Proyecto;
import com.smartpoweron.gestion_materiales.exception.ReglaNegocioException;
import com.smartpoweron.gestion_materiales.repository.MaterialRepository;
import com.smartpoweron.gestion_materiales.repository.OrdenCompraRepository;
import com.smartpoweron.gestion_materiales.repository.ProyectoRepository;
import com.smartpoweron.gestion_materiales.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/almacen")
public class InventarioController {

    @Autowired private InventarioService inventarioService;
    @Autowired private OrdenCompraRepository ordenCompraRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private ProyectoRepository proyectoRepository;

    @GetMapping("/ingresos")
    public String vistaIngresos(Model model) {
        List<OrdenCompra> ordenesAbiertas = ordenCompraRepository.findByEstado("ABIERTA");
        List<Material> materiales = materialRepository.findAll();
        List<Proyecto> proyectos = proyectoRepository.findAll();

        model.addAttribute("ordenes", ordenesAbiertas);
        model.addAttribute("materiales", materiales);
        model.addAttribute("proyectos", proyectos);
        
        // Si los DTO no existen en el modelo (ej. primera carga), los inicializamos
        if (!model.containsAttribute("ingresoDTO")) {
            model.addAttribute("ingresoDTO", new IngresoMaterialDTO());
        }
        if (!model.containsAttribute("reposicionDTO")) {
            model.addAttribute("reposicionDTO", new ReposicionMaterialDTO());
        }
        
        return "almacen/ingresos"; 
    }

    @PostMapping("/ingresos/guardar")
    public String guardarIngreso(@Valid @ModelAttribute("ingresoDTO") IngresoMaterialDTO dto,
                                 BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Verifique los datos del formulario de ingreso.");
            return "redirect:/almacen/ingresos";
        }
        try {
            inventarioService.registrarIngreso(dto);
            redirectAttributes.addFlashAttribute("mensaje", "Ingreso registrado correctamente.");
        } catch (ReglaNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/almacen/ingresos";
    }

    @PostMapping("/reposicion/guardar")
    public String guardarReposicion(@Valid @ModelAttribute("reposicionDTO") ReposicionMaterialDTO dto,
                                    BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Verifique los datos de la reposición.");
            return "redirect:/almacen/ingresos";
        }
        try {
            inventarioService.registrarReposicion(dto);
            redirectAttributes.addFlashAttribute("mensaje", "Reposición registrada y stock actualizado.");
        } catch (ReglaNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/almacen/ingresos";
    }
}