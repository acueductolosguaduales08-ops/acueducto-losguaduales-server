package com.acueducto.backend.service;

import com.acueducto.backend.dto.response.InformeAsociadoResponse;
import com.acueducto.backend.dto.response.InformePeriodoResponse;
import com.acueducto.backend.entity.Configuracion;
import com.acueducto.backend.entity.Factura;
import com.acueducto.backend.entity.Recibo;
import com.acueducto.backend.entity.enums.TipoArchivoInstitucional;
import com.acueducto.backend.service.ConfiguracionService.ImagenDocumento;
import com.acueducto.backend.util.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

/**
 * Renderiza facturas, recibos e informes usando las plantillas Thymeleaf oficiales
 * (7.9 / 8.12), garantizando que la version HTML (consulta en linea) y la version PDF
 * (descarga) contengan exactamente la misma informacion (7.10 / 7.13 / 8.13), y que
 * los informes usen los mismos logos institucionales que facturas y recibos.
 */
@Service
@RequiredArgsConstructor
public class DocumentoService {

    // Cajas de tamano fijo (en pixeles) para el logo/firma/sello en cada documento — deben
    // coincidir con el max-width/max-height declarado en el CSS de cada plantilla, para que
    // el margen y el tamano se vean identicos tanto en "ver HTML" como al descargar el PDF.
    private static final int LOGO_CAJA_ANCHO = 90;
    private static final int LOGO_CAJA_ALTO = 90;
    private static final int LOGO_CAJA_ANCHO_RECIBO = 70;
    private static final int LOGO_CAJA_ALTO_RECIBO = 70;
    private static final int FIRMA_SELLO_CAJA_ANCHO = 140;
    private static final int FIRMA_SELLO_CAJA_ALTO = 55;

    private final PdfGeneratorService pdfGeneratorService;
    private final ConfiguracionService configuracionService;

    public String renderizarFacturaHtml(Factura factura) {
        return pdfGeneratorService.renderizarHtml("factura", construirContextoFactura(factura));
    }

    public byte[] generarFacturaPdf(Factura factura) {
        return pdfGeneratorService.generarPdf("factura", construirContextoFactura(factura));
    }

    public String renderizarReciboHtml(Recibo recibo) {
        return pdfGeneratorService.renderizarHtml("recibo", construirContextoRecibo(recibo));
    }

    public byte[] generarReciboPdf(Recibo recibo) {
        return pdfGeneratorService.generarPdf("recibo", construirContextoRecibo(recibo));
    }

    public String renderizarInformePeriodoHtml(InformePeriodoResponse informe) {
        return pdfGeneratorService.renderizarHtml("informe-periodo", construirContextoInformePeriodo(informe));
    }

    public byte[] generarInformePeriodoPdf(InformePeriodoResponse informe) {
        return pdfGeneratorService.generarPdf("informe-periodo", construirContextoInformePeriodo(informe));
    }

    public String renderizarInformeAsociadoHtml(InformeAsociadoResponse informe) {
        return pdfGeneratorService.renderizarHtml("informe-asociado", construirContextoInformeAsociado(informe));
    }

    public byte[] generarInformeAsociadoPdf(InformeAsociadoResponse informe) {
        return pdfGeneratorService.generarPdf("informe-asociado", construirContextoInformeAsociado(informe));
    }

    private Context construirContextoFactura(Factura factura) {
        Configuracion config = configuracionService.obtenerEntidad();
        Context context = new Context();
        context.setVariable("factura", factura);
        context.setVariable("asociado", factura.getAsociado());
        context.setVariable("config", config);
        context.setVariable("conceptos", factura.getConceptos());
        agregarImagenesInstitucionales(context, config, LOGO_CAJA_ANCHO, LOGO_CAJA_ALTO, true);
        return context;
    }

    private Context construirContextoRecibo(Recibo recibo) {
        Configuracion config = configuracionService.obtenerEntidad();
        Context context = new Context();
        context.setVariable("recibo", recibo);
        context.setVariable("pago", recibo.getPago());
        context.setVariable("factura", recibo.getFactura());
        context.setVariable("asociado", recibo.getAsociado());
        context.setVariable("config", config);
        agregarImagenesInstitucionales(context, config, LOGO_CAJA_ANCHO_RECIBO, LOGO_CAJA_ALTO_RECIBO, false);
        return context;
    }

    private Context construirContextoInformePeriodo(InformePeriodoResponse informe) {
        Configuracion config = configuracionService.obtenerEntidad();
        Context context = new Context();
        context.setVariable("informe", informe);
        context.setVariable("config", config);
        agregarImagenesInstitucionales(context, config, LOGO_CAJA_ANCHO, LOGO_CAJA_ALTO, true);
        return context;
    }

    private Context construirContextoInformeAsociado(InformeAsociadoResponse informe) {
        Configuracion config = configuracionService.obtenerEntidad();
        Context context = new Context();
        context.setVariable("informe", informe);
        context.setVariable("config", config);
        agregarImagenesInstitucionales(context, config, LOGO_CAJA_ANCHO, LOGO_CAJA_ALTO, true);
        return context;
    }

    /**
     * Resuelve logo (y, si aplica, firma/sello) a un tamano FIJO en pixeles (no solo un maximo
     * por CSS) y lo deja disponible en el contexto como logoSrc/logoAncho/logoAlto (e igual
     * para firma/sello). Las plantillas fijan esos valores como atributos width/height del
     * <img>, no solo como CSS: eso es lo que garantiza que el margen y el tamano del logo se
     * vean exactamente igual tanto en "ver HTML" como en el PDF descargado, sin importar la
     * resolucion original de la imagen registrada por URL.
     */
    private void agregarImagenesInstitucionales(Context context, Configuracion config,
                                                 int logoCajaAncho, int logoCajaAlto, boolean incluirFirmaYSello) {
        setImagen(context, "logo", configuracionService.resolverImagenParaDocumento(
                config.getLogoActivo(), TipoArchivoInstitucional.LOGO, logoCajaAncho, logoCajaAlto));

        if (incluirFirmaYSello) {
            setImagen(context, "firma", configuracionService.resolverImagenParaDocumento(
                    config.getFirmaActiva(), TipoArchivoInstitucional.FIRMA, FIRMA_SELLO_CAJA_ANCHO, FIRMA_SELLO_CAJA_ALTO));
            setImagen(context, "sello", configuracionService.resolverImagenParaDocumento(
                    config.getSelloActivo(), TipoArchivoInstitucional.SELLO, FIRMA_SELLO_CAJA_ANCHO, FIRMA_SELLO_CAJA_ALTO));
        }
    }

    private void setImagen(Context context, String prefijo, ImagenDocumento imagen) {
        context.setVariable(prefijo + "Src", imagen != null ? imagen.src() : null);
        context.setVariable(prefijo + "Ancho", imagen != null ? imagen.ancho() : null);
        context.setVariable(prefijo + "Alto", imagen != null ? imagen.alto() : null);
    }
}

