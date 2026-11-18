package com.smartpoweron.gestion_materiales.service;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.entity.Empresa;
import com.smartpoweron.gestion_materiales.entity.Proyecto;
import com.smartpoweron.gestion_materiales.exception.ReglaNegocioException;
import com.smartpoweron.gestion_materiales.repository.EmpresaRepository;
import com.smartpoweron.gestion_materiales.repository.ProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** RF12 - registro de proyectos de tablero (§7.2 - M2). */
@Service
public class ProyectoService {

    private final ProyectoRepository repo;
    private final EmpresaRepository empresaRepo;

    public ProyectoService(ProyectoRepository repo, EmpresaRepository empresaRepo) {
        this.repo = repo;
        this.empresaRepo = empresaRepo;
    }

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
        String cliente = req.getCliente();
        Long idEmpresa = req.getIdEmpresa();
        if (idEmpresa != null) {
            Empresa emp = empresaRepo.findById(idEmpresa).orElseThrow(() ->
                    new ReglaNegocioException("La empresa (cliente) seleccionada no existe."));
            if (!"CLIENTE".equalsIgnoreCase(emp.getTipo())) {
                throw new ReglaNegocioException("La empresa seleccionada no es un cliente.");
            }
            cliente = emp.getRazonSocial();
        } else if (cliente == null || cliente.isBlank()) {
            throw new ReglaNegocioException("Debe seleccionar un cliente registrado o ingresar su nombre.");
        }
        Proyecto p = Proyecto.builder()
                .codigo(req.getCodigo())
                .cliente(cliente)
                .idEmpresa(idEmpresa)
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
        String cliente = req.getCliente();
        Long idEmpresa = req.getIdEmpresa();
        if (idEmpresa != null) {
            Empresa emp = empresaRepo.findById(idEmpresa).orElseThrow(() ->
                    new ReglaNegocioException("La empresa (cliente) seleccionada no existe."));
            if (!"CLIENTE".equalsIgnoreCase(emp.getTipo())) {
                throw new ReglaNegocioException("La empresa seleccionada no es un cliente.");
            }
            cliente = emp.getRazonSocial();
        }
        p.setCodigo(req.getCodigo());
        if (cliente != null && !cliente.isBlank()) {
            p.setCliente(cliente);
        }
        p.setIdEmpresa(idEmpresa);
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
        String ruc = null;
        if (p.getIdEmpresa() != null) {
            ruc = empresaRepo.findById(p.getIdEmpresa())
                    .map(Empresa::getRuc).orElse(null);
        }
        return Dtos.ProyectoDto.builder()
                .idProyecto(p.getIdProyecto())
                .codigo(p.getCodigo())
                .cliente(p.getCliente())
                .idEmpresa(p.getIdEmpresa())
                .rucCliente(ruc)
                .fechaInicio(p.getFechaInicio())
                .presupuesto(p.getPresupuesto())
                .estado(p.getEstado())
                .build();
    }
}
