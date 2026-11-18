package com.smartpoweron.gestion_materiales.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.smartpoweron.gestion_materiales.dto.Dtos;
import com.smartpoweron.gestion_materiales.entity.Proyecto;
import com.smartpoweron.gestion_materiales.exception.ReglaNegocioException;
import com.smartpoweron.gestion_materiales.repository.ProyectoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Generación del certificado de conformidad en PDF (RF28, R-CAL-02).
 *
 * El PDF sólo se genera cuando la última medición de cada parámetro del
 * proyecto es CONFORME; la verificación la hace CalidadService.puedeCertificar.
 * El documento se arma desde una plantilla Thymeleaf (certificado.html) y se
 * convierte con openhtmltopdf.
 */
@Service
public class CertificadoService {

    private final CalidadService calidadService;
    private final ProyectoRepository proyectoRepo;
    private final TemplateEngine templateEngine;

    @Value("${app.empresa.nombre:Smart Power On S.A.C.}")
    private String empresaNombre;
    @Value("${app.empresa.ruc:20606120860}")
    private String empresaRuc;
    @Value("${app.empresa.direccion}")
    private String empresaDireccion;

    public CertificadoService(CalidadService calidadService,
                              ProyectoRepository proyectoRepo,
                              TemplateEngine templateEngine) {
        this.calidadService = calidadService;
        this.proyectoRepo = proyectoRepo;
        this.templateEngine = templateEngine;
    }

    @Transactional
    public byte[] generarCertificado(Long idProyecto) {
        // R-CAL-02 / RF28: sólo si la última medición de cada parámetro es CONFORME
        if (!calidadService.puedeCertificar(idProyecto)) {
            throw new ReglaNegocioException(
                    "No se puede emitir el certificado: hay parámetros con última medición NO_CONFORME o sin medir.");
        }
        Proyecto p = proyectoRepo.findById(idProyecto).orElseThrow(() ->
                new ReglaNegocioException("El proyecto no existe."));
        List<Dtos.PruebaCalidadDto> ultimas = calidadService.listarPorProyecto(idProyecto).stream()
                .filter(prueba -> !Boolean.TRUE.equals(prueba.getAnulada()))
                .toList();

        Context ctx = new Context(Locale.forLanguageTag("es-PE"));
        ctx.setVariable("empresa", empresaNombre);
        ctx.setVariable("ruc", empresaRuc);
        ctx.setVariable("direccion", empresaDireccion);
        ctx.setVariable("proyecto", p);
        ctx.setVariable("pruebas", ultimas);
        ctx.setVariable("fecha", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-PE"))));
        ctx.setVariable("titulo", "Certificado de Conformidad");

        String html = templateEngine.process("calidad/certificado", ctx);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ReglaNegocioException("No se pudo generar el PDF: " + e.getMessage());
        }
    }
}
