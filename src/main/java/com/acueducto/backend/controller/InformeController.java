package com.acueducto.backend.controller;

import com.acueducto.backend.dto.response.InformeAsociadoResponse;
import com.acueducto.backend.dto.response.InformePeriodoResponse;
import com.acueducto.backend.service.DocumentoService;
import com.acueducto.backend.service.InformeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Modulo de Informes: reportes mensuales, anuales y de seguimiento a un asociado,
 * en HTML (consulta en linea) o PDF (descarga), con la misma informacion en ambos
 * formatos y los mismos logos institucionales que facturas y recibos. Exclusivo de
 * Tesorero y Administrador.
 */
@Tag(name = "16. Informes", description = "Reportes mensuales, anuales y de seguimiento a un asociado (HTML/PDF). Tesorero y Administrador.")
@RestController
@RequestMapping("/api/v1/informes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
public class InformeController {

    private final InformeService informeService;
    private final DocumentoService documentoService;

    @Operation(summary = "Datos del informe mensual (JSON)", description = "Util para previsualizar los datos antes de generar el documento.")
    @GetMapping("/periodo/mes/{mesContableId}")
    public ResponseEntity<InformePeriodoResponse> datosInformeMes(@PathVariable Long mesContableId) {
        return ResponseEntity.ok(informeService.generarInformeMes(mesContableId));
    }

    @Operation(summary = "Informe mensual en HTML")
    @GetMapping(value = "/periodo/mes/{mesContableId}/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> informeMesHtml(@PathVariable Long mesContableId) {
        InformePeriodoResponse informe = informeService.generarInformeMes(mesContableId);
        return ResponseEntity.ok(documentoService.renderizarInformePeriodoHtml(informe));
    }

    @Operation(summary = "Informe mensual en PDF")
    @GetMapping(value = "/periodo/mes/{mesContableId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> informeMesPdf(@PathVariable Long mesContableId) {
        InformePeriodoResponse informe = informeService.generarInformeMes(mesContableId);
        byte[] pdf = documentoService.generarInformePeriodoPdf(informe);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=informe-" + informe.getTituloPeriodo().replace(" ", "-") + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Datos del informe anual (JSON)")
    @GetMapping("/periodo/anio/{anioContableId}")
    public ResponseEntity<InformePeriodoResponse> datosInformeAnio(@PathVariable Long anioContableId) {
        return ResponseEntity.ok(informeService.generarInformeAnio(anioContableId));
    }

    @Operation(summary = "Informe anual en HTML")
    @GetMapping(value = "/periodo/anio/{anioContableId}/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> informeAnioHtml(@PathVariable Long anioContableId) {
        InformePeriodoResponse informe = informeService.generarInformeAnio(anioContableId);
        return ResponseEntity.ok(documentoService.renderizarInformePeriodoHtml(informe));
    }

    @Operation(summary = "Informe anual en PDF")
    @GetMapping(value = "/periodo/anio/{anioContableId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> informeAnioPdf(@PathVariable Long anioContableId) {
        InformePeriodoResponse informe = informeService.generarInformeAnio(anioContableId);
        byte[] pdf = documentoService.generarInformePeriodoPdf(informe);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=informe-" + informe.getTituloPeriodo().replace(" ", "-") + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Datos del informe de seguimiento a un asociado (JSON)")
    @GetMapping("/asociado/{asociadoId}")
    public ResponseEntity<InformeAsociadoResponse> datosInformeAsociado(@PathVariable Long asociadoId) {
        return ResponseEntity.ok(informeService.generarInformeAsociado(asociadoId));
    }

    @Operation(summary = "Informe de seguimiento a un asociado en HTML")
    @GetMapping(value = "/asociado/{asociadoId}/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> informeAsociadoHtml(@PathVariable Long asociadoId) {
        InformeAsociadoResponse informe = informeService.generarInformeAsociado(asociadoId);
        return ResponseEntity.ok(documentoService.renderizarInformeAsociadoHtml(informe));
    }

    @Operation(summary = "Informe de seguimiento a un asociado en PDF")
    @GetMapping(value = "/asociado/{asociadoId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> informeAsociadoPdf(@PathVariable Long asociadoId) {
        InformeAsociadoResponse informe = informeService.generarInformeAsociado(asociadoId);
        byte[] pdf = documentoService.generarInformeAsociadoPdf(informe);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=informe-" + informe.getCodigoInterno() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
