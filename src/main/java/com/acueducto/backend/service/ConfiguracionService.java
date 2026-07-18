package com.acueducto.backend.service;

import com.acueducto.backend.dto.request.ConfiguracionRequest;
import com.acueducto.backend.dto.request.MetodoPagoRequest;
import com.acueducto.backend.dto.response.ConfiguracionResponse;
import com.acueducto.backend.entity.ArchivoInstitucional;
import com.acueducto.backend.entity.Configuracion;
import com.acueducto.backend.entity.MetodoPago;
import com.acueducto.backend.entity.enums.FuenteArchivoInstitucional;
import com.acueducto.backend.entity.enums.TipoArchivoInstitucional;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.repository.ArchivoInstitucionalRepository;
import com.acueducto.backend.repository.ConfiguracionRepository;
import com.acueducto.backend.repository.MetodoPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Modulo de Parametros del Sistema (Modulo 10). Centraliza informacion institucional,
 * tarifas, datos bancarios, metodos de pago y archivos institucionales (logo, firma, sello).
 */
@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final ArchivoInstitucionalRepository archivoInstitucionalRepository;
    private final AuditoriaService auditoriaService;

    public ConfiguracionResponse obtener() {
        return ConfiguracionResponse.fromEntity(obtenerEntidad());
    }

    public Configuracion obtenerEntidad() {
        return configuracionRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("La configuracion del sistema aun no ha sido inicializada."));
    }

    @Transactional
    public ConfiguracionResponse actualizar(ConfiguracionRequest request) {
        Configuracion config = configuracionRepository.findAll().stream().findFirst()
                .orElseGet(() -> Configuracion.builder().build());

        config.setNombreAcueducto(request.nombreAcueducto());
        config.setNit(request.nit());
        config.setDireccion(request.direccion());
        config.setTelefonoPrincipal(request.telefonoPrincipal());
        config.setCorreo(request.correo());
        config.setMunicipio(request.municipio());
        config.setDepartamento(request.departamento());
        config.setBanco(request.banco());
        config.setTipoCuenta(request.tipoCuenta());
        config.setNumeroCuenta(request.numeroCuenta());
        config.setTitularCuenta(request.titularCuenta());
        config.setValorM3(request.valorM3());
        config.setCargoFijoAdministracion(request.cargoFijoAdministracion());
        config.setValorReconexion(request.valorReconexion());
        config.setValorMultaDefecto(request.valorMultaDefecto());
        if (request.diasPlazoPago() != null) config.setDiasPlazoPago(request.diasPlazoPago());
        config.setNotasFactura(request.notasFactura());

        config = configuracionRepository.save(config);
        auditoriaService.registrar("ACTUALIZAR_CONFIGURACION", "CONFIGURACION", "parametros_generales", null);
        return ConfiguracionResponse.fromEntity(config);
    }

    /** Numeracion consecutiva y atomica de facturas, recibos, entradas y salidas (4.9). */
    @Transactional
    public synchronized long siguienteNumeroFactura() {
        Configuracion config = obtenerEntidad();
        long numero = config.getSiguienteNumeroFactura();
        config.setSiguienteNumeroFactura(numero + 1);
        configuracionRepository.save(config);
        return numero;
    }

    @Transactional
    public synchronized long siguienteNumeroRecibo() {
        Configuracion config = obtenerEntidad();
        long numero = config.getSiguienteNumeroRecibo();
        config.setSiguienteNumeroRecibo(numero + 1);
        configuracionRepository.save(config);
        return numero;
    }

    // ---- Metodos de pago ----

    @Transactional
    public MetodoPago crearMetodoPago(MetodoPagoRequest request) {
        MetodoPago metodo = MetodoPago.builder().nombre(request.nombre()).activo(true).build();
        return metodoPagoRepository.save(metodo);
    }

    @Transactional
    public MetodoPago cambiarEstadoMetodoPago(Long id, boolean activo) {
        MetodoPago metodo = metodoPagoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Metodo de pago no encontrado"));
        metodo.setActivo(activo);
        return metodoPagoRepository.save(metodo);
    }

    public List<MetodoPago> listarMetodosPagoActivos() {
        return metodoPagoRepository.findByActivoTrue();
    }

    public List<MetodoPago> listarTodosMetodosPago() {
        return metodoPagoRepository.findAll();
    }

    // ---- Archivos institucionales (logo, firma, sello) ----
    //
    // Igual que las imagenes de publicaciones: SOLO se manejan por URL. No hay subida de
    // archivo al servidor ni almacenamiento local — eso es lo que antes se perdia/fallaba.
    // El administrador copia el enlace de una imagen (de donde sea) y la registra; puede
    // registrar varias para un mismo tipo (logo/firma/sello) y elegir cual esta activa.
    //
    // Para que igual el PDF y la vista HTML muestren siempre el logo correctamente (sin
    // depender de que el enlace externo responda justo en ese instante) y para que una foto
    // en alta resolucion no infle el peso del documento, al generar una factura/recibo/informe
    // la URL activa se descarga EN MEMORIA solo para ese documento puntual, se ajusta su
    // resolucion si hace falta, y se incrusta como imagen — nada de esto se guarda en el
    // servidor ni en la base de datos (ver resolverImagenParaDocumento). Si la URL no responde
    // en ese momento, el documento igual se genera, simplemente sin el logo.

    /** Ancho/alto maximo (px) al que se ajusta cada tipo de archivo institucional al incrustarlo
     *  en un documento, para que una imagen de resolucion muy alta no infle el PDF/HTML. */
    private static final int LOGO_MAX_DIMENSION_PX = 480;
    private static final int FIRMA_SELLO_MAX_DIMENSION_PX = 320;

    /** Peso objetivo tras comprimir; si se supera, se reduce calidad/tamano de forma progresiva. */
    private static final long PESO_OBJETIVO_BYTES = 300 * 1024;

    /** Deja margen bajo el limite de 200 caracteres de la columna nombre_archivo. */
    private static final int NOMBRE_ARCHIVO_MAX_LEN = 150;

    private static final Pattern CARACTER_INSEGURO_URL =
            Pattern.compile("[^A-Za-z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]");

    @Transactional
    public ArchivoInstitucional subirArchivoInstitucionalDesdeUrl(TipoArchivoInstitucional tipo, String url, String nombreArchivo) {
        // No se descarga ni se guarda ninguna copia en el servidor; solo se valida/normaliza
        // la URL (aceptando espacios, comillas y URLs largas, que era el bug original) y se
        // guarda tal cual, igual que el campo imagenUrl de las publicaciones.
        String urlNormalizada = normalizarUrl(url);
        String nombreSugerido = (nombreArchivo == null || nombreArchivo.isBlank())
                ? obtenerNombreDesdeUrl(urlNormalizada)
                : nombreArchivo;
        String nombreFinal = sanitizarNombre(nombreSugerido);

        ArchivoInstitucional entidad = ArchivoInstitucional.builder()
                .tipo(tipo)
                .nombreArchivo(nombreFinal)
                .ruta(urlNormalizada)
                .fuente(FuenteArchivoInstitucional.URL)
                .activo(false)
                .build();
        entidad = archivoInstitucionalRepository.save(entidad);

        auditoriaService.registrar("REGISTRAR_ARCHIVO_INSTITUCIONAL_URL", "CONFIGURACION", nombreFinal, tipo.name());
        return entidad;
    }

    /** Selecciona el archivo activo de un tipo sin necesidad de modificar el codigo (10.10). */
    @Transactional
    public void activarArchivoInstitucional(Long archivoId) {
        ArchivoInstitucional archivo = archivoInstitucionalRepository.findById(archivoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Archivo institucional no encontrado"));

        archivoInstitucionalRepository.findByTipo(archivo.getTipo())
                .forEach(a -> {
                    a.setActivo(a.getId().equals(archivoId));
                    archivoInstitucionalRepository.save(a);
                });

        Configuracion config = configuracionRepository.findAll().stream().findFirst()
                .orElseGet(() -> Configuracion.builder().build());
        switch (archivo.getTipo()) {
            case LOGO -> config.setLogoActivo(archivo.getRuta());
            case FIRMA -> config.setFirmaActiva(archivo.getRuta());
            case SELLO -> config.setSelloActivo(archivo.getRuta());
        }
        configuracionRepository.save(config);

        auditoriaService.registrar("ACTIVAR_ARCHIVO_INSTITUCIONAL", "CONFIGURACION", archivo.getNombreArchivo(), archivo.getTipo().name());
    }

    /** Quita una URL registrada de la lista. Si era la activa, la config queda apuntando a una
     *  referencia que ya no esta en la lista (no se borra sola de config.*Activo) hasta que el
     *  administrador active otra; esto evita que un documento a medio generar se quede sin logo
     *  de golpe por un borrado accidental. */
    @Transactional
    public void eliminarArchivoInstitucional(Long archivoId) {
        ArchivoInstitucional archivo = archivoInstitucionalRepository.findById(archivoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Archivo institucional no encontrado"));
        archivoInstitucionalRepository.delete(archivo);
        auditoriaService.registrar("ELIMINAR_ARCHIVO_INSTITUCIONAL", "CONFIGURACION", archivo.getNombreArchivo(), archivo.getTipo().name());
    }

    public List<ArchivoInstitucional> listarArchivosPorTipo(TipoArchivoInstitucional tipo) {
        return archivoInstitucionalRepository.findByTipo(tipo);
    }

    /** Cache en memoria (no persistida) de logos/firmas/sellos registrados por URL, para no
     *  volver a descargarlos en cada documento generado dentro de la misma ventana de tiempo
     *  (ej. una corrida de facturacion mensual que genera muchas facturas seguidas). Guarda el
     *  dato ya comprimido junto con la resolucion ORIGINAL de la imagen (antes de ajustarla a
     *  ninguna caja de tamano), para poder recalcular el ancho/alto exacto en cada llamada sin
     *  tener que descargarla de nuevo. */
    private final ConcurrentHashMap<String, CacheImagenUrl> cacheImagenesUrl = new ConcurrentHashMap<>();
    private static final long CACHE_IMAGEN_URL_TTL_MS = 10 * 60 * 1000; // 10 minutos

    private record CacheImagenUrl(String dataUri, int anchoOriginal, int altoOriginal, long expiraEn) {
    }

    /** Resultado listo para usar en la plantilla: la imagen (data URI) y el ancho/alto exactos
     *  en pixeles a los que debe fijarse en el HTML/PDF. */
    public record ImagenDocumento(String src, int ancho, int alto) {
    }

    /**
     * Resuelve el logo/firma/sello activo (siempre una URL) para usarlo en la version HTML o en
     * el PDF de un documento (factura, recibo, informe): la descarga en memoria solo para ese
     * documento, calcula el tamano exacto (en pixeles) al que debe verse dentro de la caja
     * indicada (cajaAncho x cajaAlto) manteniendo su proporcion, y la entrega como data URI
     * temporal — nada de esto se escribe a disco ni se guarda en la base de datos.
     *
     * El ancho/alto que devuelve se pensaron para fijarse como atributos width/height del
     * <img> (no solo como CSS max-width/max-height): el generador de PDF no siempre respeta
     * max-width/max-height con la misma fidelidad que un navegador, y una imagen de resolucion
     * alta puede terminar imprimiendose mucho mas grande de lo esperado aunque en el HTML se
     * vea bien. Fijar el tamano exacto en pixeles es lo que garantiza un resultado identico
     * (y con margenes consistentes) en ambos casos.
     *
     * Si la URL no responde al momento de generar el documento, no se interrumpe la generacion:
     * se devuelve null y simplemente no se muestra ese logo/firma/sello.
     */
    public ImagenDocumento resolverImagenParaDocumento(String ruta, TipoArchivoInstitucional tipo,
                                                        int cajaAncho, int cajaAlto) {
        if (ruta == null || ruta.isBlank() || !ruta.matches("(?i)^https?://.+")) {
            return null;
        }
        CacheImagenUrl datos = obtenerODescargarImagen(ruta, tipo);
        if (datos == null) {
            return null;
        }
        int[] dimension = ajustarDentroDeCaja(datos.anchoOriginal(), datos.altoOriginal(), cajaAncho, cajaAlto);
        return new ImagenDocumento(datos.dataUri(), dimension[0], dimension[1]);
    }

    private CacheImagenUrl obtenerODescargarImagen(String url, TipoArchivoInstitucional tipo) {
        CacheImagenUrl enCache = cacheImagenesUrl.get(url);
        if (enCache != null && enCache.expiraEn() > System.currentTimeMillis()) {
            return enCache;
        }
        try {
            byte[] bytes = descargarBytesTransitorio(url);
            if (bytes == null || bytes.length == 0) {
                return null;
            }

            CacheImagenUrl resultado;
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(bytes));
            if (imagen == null) {
                // No es un raster reconocible (puede ser SVG); si pesa poco, se usa tal cual,
                // sin guardarlo en ningun lado. No se conoce su resolucion intrinseca, asi que
                // se usara el tamano de la caja solicitada tal cual (ver ajustarDentroDeCaja).
                if (bytes.length > PESO_OBJETIVO_BYTES) {
                    return null;
                }
                String mime = pareceSvgBytes(bytes) ? "image/svg+xml" : "image/png";
                String dataUri = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
                resultado = new CacheImagenUrl(dataUri, 0, 0, System.currentTimeMillis() + CACHE_IMAGEN_URL_TTL_MS);
            } else {
                boolean transparencia = imagen.getColorModel().hasAlpha();
                byte[] comprimido = redimensionarYComprimirABytes(imagen, dimensionMaximaPara(tipo), transparencia);
                String mime = transparencia ? "image/png" : "image/jpeg";
                String dataUri = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(comprimido);
                resultado = new CacheImagenUrl(dataUri, imagen.getWidth(), imagen.getHeight(),
                        System.currentTimeMillis() + CACHE_IMAGEN_URL_TTL_MS);
            }

            cacheImagenesUrl.put(url, resultado);
            return resultado;
        } catch (Exception e) {
            // Si la URL no esta disponible al momento de generar el documento, no se interrumpe
            // la generacion del PDF/HTML: simplemente no se muestra ese logo/firma/sello.
            return null;
        }
    }

    /** Calcula el ancho/alto (en pixeles) al que debe verse una imagen para que quepa dentro de
     *  una caja de cajaAncho x cajaAlto sin deformarse ni recortarse. Nunca la agranda mas alla
     *  de su tamano original (evita logos borrosos por estirar una imagen pequena). */
    private int[] ajustarDentroDeCaja(int anchoOriginal, int altoOriginal, int cajaAncho, int cajaAlto) {
        if (anchoOriginal <= 0 || altoOriginal <= 0) {
            return new int[]{cajaAncho, cajaAlto};
        }
        double escala = Math.min((double) cajaAncho / anchoOriginal, (double) cajaAlto / altoOriginal);
        escala = Math.min(escala, 1.0);
        int ancho = Math.max(1, (int) Math.round(anchoOriginal * escala));
        int alto = Math.max(1, (int) Math.round(altoOriginal * escala));
        return new int[]{ancho, alto};
    }

    private byte[] descargarBytesTransitorio(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(8000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; acueducto-backend-doc-render/1.0)");
        conn.connect();
        try {
            if (conn.getResponseCode() != 200) {
                return null;
            }
            try (InputStream is = conn.getInputStream()) {
                return is.readAllBytes();
            }
        } finally {
            conn.disconnect();
        }
    }

    private boolean pareceSvgBytes(byte[] bytes) {
        int len = Math.min(bytes.length, 300);
        String inicio = new String(bytes, 0, len, StandardCharsets.UTF_8).stripLeading().toLowerCase();
        return inicio.startsWith("<svg") || (inicio.startsWith("<?xml") && inicio.contains("<svg"));
    }

    private int dimensionMaximaPara(TipoArchivoInstitucional tipo) {
        return tipo == TipoArchivoInstitucional.LOGO ? LOGO_MAX_DIMENSION_PX : FIRMA_SELLO_MAX_DIMENSION_PX;
    }

    /** Redimensiona/comprime en memoria (sin tocar disco); la usa el resuelto transitorio de
     *  imagenes por URL al generar cada documento. */
    private byte[] redimensionarYComprimirABytes(BufferedImage imagen, int dimensionMaxima,
                                                  boolean transparencia) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        if (transparencia) {
            int dimension = dimensionMaxima;
            net.coobird.thumbnailator.Thumbnails.of(imagen)
                    .size(dimension, dimension)
                    .outputFormat("png")
                    .toOutputStream(buffer);
            int intentos = 0;
            while (buffer.size() > PESO_OBJETIVO_BYTES && intentos < 4 && dimension > 60) {
                dimension = (int) (dimension * 0.75);
                buffer.reset();
                net.coobird.thumbnailator.Thumbnails.of(imagen)
                        .size(dimension, dimension)
                        .outputFormat("png")
                        .toOutputStream(buffer);
                intentos++;
            }
        } else {
            double calidad = 0.85;
            net.coobird.thumbnailator.Thumbnails.of(imagen)
                    .size(dimensionMaxima, dimensionMaxima)
                    .outputFormat("jpg")
                    .outputQuality(calidad)
                    .toOutputStream(buffer);
            int intentos = 0;
            while (buffer.size() > PESO_OBJETIVO_BYTES && intentos < 5 && calidad > 0.35) {
                calidad -= 0.15;
                buffer.reset();
                net.coobird.thumbnailator.Thumbnails.of(imagen)
                        .size(dimensionMaxima, dimensionMaxima)
                        .outputFormat("jpg")
                        .outputQuality(calidad)
                        .toOutputStream(buffer);
                intentos++;
            }
        }
        return buffer.toByteArray();
    }

    /**
     * Limpia un nombre de archivo (viene de la ultima parte de una URL, o de un nombre que el
     * administrador escribio a mano): quita separadores de ruta, query string, comillas y
     * caracteres de control, y lo recorta a una longitud segura para que nunca exceda el limite
     * de la columna nombre_archivo (200). Esto es lo que antes fallaba con URLs firmadas/largas
     * (tipo S3) que traian nombres de cientos de caracteres.
     */
    private String sanitizarNombre(String nombre) {
        String base = (nombre == null || nombre.isBlank()) ? "archivo" : nombre.trim();
        base = base.replace("\\", "/");
        if (base.contains("/")) {
            base = base.substring(base.lastIndexOf('/') + 1);
        }
        int q = base.indexOf('?');
        if (q >= 0) base = base.substring(0, q);
        int h = base.indexOf('#');
        if (h >= 0) base = base.substring(0, h);

        base = base.replaceAll("[\"'<>|:*]", "");
        base = base.replaceAll("\\s+", "_");
        base = base.replaceAll("[^a-zA-Z0-9._-]", "");
        if (base.isBlank()) {
            base = "archivo";
        }

        if (base.length() > NOMBRE_ARCHIVO_MAX_LEN) {
            String ext = "";
            int dot = base.lastIndexOf('.');
            if (dot > 0 && dot > base.length() - 6) {
                ext = base.substring(dot);
                base = base.substring(0, dot);
            }
            int recorte = Math.max(1, NOMBRE_ARCHIVO_MAX_LEN - ext.length());
            base = base.substring(0, Math.min(base.length(), recorte)) + ext;
        }
        return base;
    }

    private String obtenerNombreDesdeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "archivo-institucional";
        }
        String sinQuery = url;
        int q = sinQuery.indexOf('?');
        if (q >= 0) sinQuery = sinQuery.substring(0, q);
        int h = sinQuery.indexOf('#');
        if (h >= 0) sinQuery = sinQuery.substring(0, h);

        String[] partes = sinQuery.split("/");
        String ultimo = partes.length > 0 ? partes[partes.length - 1] : "";
        if (ultimo.isBlank()) {
            return "archivo-institucional";
        }
        try {
            ultimo = URLDecoder.decode(ultimo, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // si no se puede decodificar, se usa tal cual; sanitizarNombre() limpia el resto
        }
        return ultimo;
    }

    /**
     * Valida y normaliza la URL ingresada por el administrador. En vez de rechazar o borrar
     * caracteres especiales (comillas, espacios, acentos, etc.), los codifica correctamente
     * para que la URL sea valida, sin importar de donde se copio. Tambien quita comillas
     * "envolventes" que suelen quedar pegadas al copiar y pegar (por ejemplo desde un atributo
     * href="..."). No impone un limite de longitud: ruta (en ArchivoInstitucional) y
     * logo_activo/firma_activa/sello_activo (en Configuracion) son columnas de tipo TEXT, asi
     * que una URL larga (por ejemplo firmada, tipo S3) ya no rompe el guardado.
     */
    private String normalizarUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("La URL no puede estar vacia");
        }

        String limpia = url.trim();
        while (limpia.length() >= 2 && esParDeComillas(limpia.charAt(0), limpia.charAt(limpia.length() - 1))) {
            limpia = limpia.substring(1, limpia.length() - 1).trim();
        }
        limpia = limpia.replace("\\", "/");

        if (!limpia.matches("(?i)^https?://.+")) {
            throw new IllegalArgumentException("La URL debe empezar por http:// o https://");
        }

        return codificarCaracteresEspeciales(limpia);
    }

    private boolean esParDeComillas(char inicio, char fin) {
        return (inicio == '"' && fin == '"')
                || (inicio == '\'' && fin == '\'')
                || (inicio == '\u201c' && fin == '\u201d')
                || (inicio == '\u2018' && fin == '\u2019')
                || (inicio == '<' && fin == '>');
    }

    /** Codifica en porcentaje cualquier caracter no valido en una URL (comillas, espacios, acentos, etc.). */
    private String codificarCaracteresEspeciales(String url) {
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (CARACTER_INSEGURO_URL.matcher(String.valueOf(c)).matches()) {
                for (byte b : String.valueOf(c).getBytes(StandardCharsets.UTF_8)) {
                    resultado.append(String.format("%%%02X", b));
                }
            } else {
                resultado.append(c);
            }
        }
        return resultado.toString();
    }
}
