package com.smartpoweron.gestion_materiales.service;

import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.entity.Empresa;
import com.smartpoweron.gestion_materiales.exception.ReglaNegocioException;
import com.smartpoweron.gestion_materiales.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de registro de empresas (proveedores y clientes).
 * Permite reutilizar los datos de la empresa (RUC, razón social, razón
 * comercial y contacto) para que el usuario no los digite cada vez que crea
 * una orden de compra o un proyecto.
 */
@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepo;

    public EmpresaService(EmpresaRepository empresaRepo) {
        this.empresaRepo = empresaRepo;
    }

    @Transactional(readOnly = true)
    public List<Dtos.EmpresaDto> listar(String tipo) {
        return (tipo == null || tipo.isBlank()
                ? empresaRepo.findAll()
                : empresaRepo.findByTipoAndEstado(tipo.toUpperCase(), "ACTIVO")).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Dtos.EmpresaDto> listarTodos(String tipo) {
        return (tipo == null || tipo.isBlank()
                ? empresaRepo.findAll()
                : empresaRepo.findByTipo(tipo.toUpperCase())).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Dtos.EmpresaDto obtener(Long id) {
        return toDto(empresaRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La empresa no existe.")));
    }

    @Transactional
    public Dtos.EmpresaDto registrar(Dtos.EmpresaCreate req) {
        String tipo = req.getTipo() == null ? "" : req.getTipo().toUpperCase();
        if (!"PROVEEDOR".equals(tipo) && !"CLIENTE".equals(tipo)) {
            throw new ReglaNegocioException("El tipo debe ser PROVEEDOR o CLIENTE.");
        }
        if (empresaRepo.existsByRuc(req.getRuc())) {
            throw new ReglaNegocioException("Ya existe una empresa registrada con el RUC " + req.getRuc());
        }
        Empresa e = Empresa.builder()
                .ruc(req.getRuc())
                .razonSocial(req.getRazonSocial())
                .razonComercial(req.getRazonComercial())
                .contactoNombre(req.getContactoNombre())
                .contactoTelefono(req.getContactoTelefono())
                .contactoCorreo(req.getContactoCorreo())
                .tipo(tipo)
                .estado("ACTIVO")
                .build();
        return toDto(empresaRepo.save(e));
    }

    @Transactional
    public Dtos.EmpresaDto actualizar(Long id, Dtos.EmpresaUpdate req) {
        Empresa e = empresaRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La empresa no existe."));
        if (empresaRepo.existsByRucAndIdEmpresaNot(req.getRuc(), id)) {
            throw new ReglaNegocioException("Ya existe una empresa registrada con el RUC " + req.getRuc());
        }
        e.setRuc(req.getRuc());
        e.setRazonSocial(req.getRazonSocial());
        e.setRazonComercial(req.getRazonComercial());
        e.setContactoNombre(req.getContactoNombre());
        e.setContactoTelefono(req.getContactoTelefono());
        e.setContactoCorreo(req.getContactoCorreo());
        if (req.getEstado() != null && !req.getEstado().isBlank()) {
            e.setEstado(req.getEstado());
        }
        return toDto(empresaRepo.save(e));
    }

    @Transactional
    public void desactivar(Long id) {
        Empresa e = empresaRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La empresa no existe."));
        if (!"ACTIVO".equals(e.getEstado())) {
            throw new ReglaNegocioException("La empresa ya está inactiva.");
        }
        e.setEstado("INACTIVO");
        empresaRepo.save(e);
    }

    @Transactional
    public void reactivar(Long id) {
        Empresa e = empresaRepo.findById(id).orElseThrow(() ->
                new ReglaNegocioException("La empresa no existe."));
        if ("ACTIVO".equals(e.getEstado())) {
            throw new ReglaNegocioException("La empresa ya está activa.");
        }
        e.setEstado("ACTIVO");
        empresaRepo.save(e);
    }

    public Dtos.EmpresaDto toDto(Empresa e) {
        return Dtos.EmpresaDto.builder()
                .idEmpresa(e.getIdEmpresa())
                .ruc(e.getRuc())
                .razonSocial(e.getRazonSocial())
                .razonComercial(e.getRazonComercial())
                .contactoNombre(e.getContactoNombre())
                .contactoTelefono(e.getContactoTelefono())
                .contactoCorreo(e.getContactoCorreo())
                .tipo(e.getTipo())
                .estado(e.getEstado())
                .fechaRegistro(e.getFechaRegistro())
                .build();
    }
}
