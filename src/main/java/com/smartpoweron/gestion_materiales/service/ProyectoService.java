package com.smartpoweron.gestion_materiales.service;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.entity.Proyecto;
import com.smartpoweron.gestion_materiales.exception.ReglaNegocioException;
import com.smartpoweron.gestion_materiales.repository.ProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** RF12 - registro de proyectos de tablero (§7.2 - M2). */
@Service
public class ProyectoService {

    private final ProyectoRepository repo;

    public ProyectoService(ProyectoRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public List<Dtos.ProyectoDto> listarProyectos() {
        return repo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public Dtos.ProyectoDto crearProyecto(Dtos.ProyectoCreate req) {
        if (repo.existsByCodigo(req.getCodigo())) {
            throw new ReglaNegocioException("El código del proyecto ya existe.");
        }
        validarDatos(req);
        Proyecto p = Proyecto.builder()
                .codigo(req.getCodigo())
                .cliente(req.getCliente())
                .fechaInicio(req.getFechaInicio())
                .presupuesto(req.getPresupuesto())
                .estado("EN_PROCESO")
                .build();
        return toDto(repo.save(p));
    }

    @Transactional
    public Dtos.ProyectoDto actualizarProyecto(Long id, Dtos.ProyectoCreate req) {
        Proyecto p = repo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("El proyecto no existe."));
        if ("ANULADO".equals(p.getEstado())) {
            throw new ReglaNegocioException("No se puede editar un proyecto anulado.");
        }
        if (repo.existsByCodigoAndIdProyectoNot(req.getCodigo(), id)) {
            throw new ReglaNegocioException("El código del proyecto ya existe.");
        }
        validarDatos(req);
        p.setCodigo(req.getCodigo());
        p.setCliente(req.getCliente());
        p.setFechaInicio(req.getFechaInicio());
        p.setPresupuesto(req.getPresupuesto());
        return toDto(repo.save(p));
    }

    @Transactional
    public void anularProyecto(Long id) {
        Proyecto p = repo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("El proyecto no existe."));
        if ("ANULADO".equals(p.getEstado())) {
            throw new ReglaNegocioException("El proyecto ya está anulado.");
        }
        p.setEstado("ANULADO");
        repo.save(p);
    }

    private void validarDatos(Dtos.ProyectoCreate req) {
        if (req.getPresupuesto() == null || req.getPresupuesto().signum() <= 0) {
            throw new ReglaNegocioException("El presupuesto debe ser mayor que cero.");
        }
        if (req.getFechaInicio() == null) {
            throw new ReglaNegocioException("La fecha de inicio es obligatoria.");
        }
    }

    public Dtos.ProyectoDto toDto(Proyecto p) {
        return Dtos.ProyectoDto.builder()
                .idProyecto(p.getIdProyecto())
                .codigo(p.getCodigo())
                .cliente(p.getCliente())
                .fechaInicio(p.getFechaInicio())
                .presupuesto(p.getPresupuesto())
                .estado(p.getEstado())
                .build();
    }
}
