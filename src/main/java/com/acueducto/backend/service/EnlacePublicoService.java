package com.acueducto.backend.service;

import com.acueducto.backend.dto.response.EnlacePublicoResponse;
import com.acueducto.backend.entity.EnlacePublicoDocumento;
import com.acueducto.backend.entity.Factura;
import com.acueducto.backend.entity.Recibo;
import com.acueducto.backend.entity.enums.EstadoFactura;
import com.acueducto.backend.entity.enums.EstadoRecibo;
import com.acueducto.backend.entity.enums.TipoDocumentoPublico;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.repository.EnlacePublicoDocumentoRepository;
import com.acueducto.backend.repository.FacturaRepository;
import com.acueducto.backend.repository.ReciboRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Genera y valida enlaces publicos temporales de descarga de facturas y recibos.
 *
 * Reglas:
 * - Solo ADMINISTRADOR y TESORERO pueden crear el enlace (se controla en el controlador).
 * - Cada documento tiene como maximo UN enlace activo: generar uno nuevo elimina el anterior.
 * - El enlace dura 72 horas por defecto (configurable); al expirar se borra definitivamente.
 * - El endpoint publico NO requiere iniciar sesion; valida token, vigencia y que el documento
 *   exista y este disponible (una factura o recibo anulado no se puede compartir).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnlacePublicoService {

    private static final String ALFABETO_TOKEN = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int LARGO_TOKEN = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EnlacePublicoDocumentoRepository enlaceRepository;
    private final FacturaRepository facturaRepository;
    private final ReciboRepository reciboRepository;

    @Value("${app.public-link.expiration-hours:72}")
    private long expirationHours;

    @Transactional
    public EnlacePublicoResponse generarEnlaceFactura(Long facturaId, String baseUrl) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada con id " + facturaId));
        return generarEnlace(TipoDocumentoPublico.FACTURA, factura.getId(), factura.getNumeroFactura(), baseUrl);
    }

    @Transactional
    public EnlacePublicoResponse generarEnlaceRecibo(String numeroRecibo, String baseUrl) {
        Recibo recibo = reciboRepository.findByNumeroRecibo(numeroRecibo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recibo no encontrado: " + numeroRecibo));
        return generarEnlace(TipoDocumentoPublico.RECIBO, recibo.getId(), recibo.getNumeroRecibo(), baseUrl);
    }

    private EnlacePublicoResponse generarEnlace(TipoDocumentoPublico tipo, Long documentoId, String numero, String baseUrl) {
        enlaceRepository.deleteByTipoDocumentoAndDocumentoId(tipo, documentoId);

        EnlacePublicoDocumento enlace = EnlacePublicoDocumento.builder()
                .token(generarToken())
                .tipoDocumento(tipo)
                .documentoId(documentoId)
                .fechaExpiracion(LocalDateTime.now().plusHours(expirationHours))
                .build();
        enlace = enlaceRepository.save(enlace);

        String url = baseUrl + "/api/v1/public/" + tipo.name().toLowerCase() + "s/" + enlace.getToken();
        return EnlacePublicoResponse.builder()
                .documentoId(documentoId)
                .numeroDocumento(numero)
                .tipo(tipo)
                .publicDownloadUrl(url)
                .expiresAt(enlace.getFechaExpiracion())
                .build();
    }

    /** Entrega la factura correspondiente a un token valido y disponible (para el PDF publico). */
    @Transactional
    public Factura obtenerFacturaPublica(String token) {
        EnlacePublicoDocumento enlace = validarEnlace(token);
        if (enlace.getTipoDocumento() != TipoDocumentoPublico.FACTURA) {
            throw new RecursoNoEncontradoException("El enlace no corresponde a una factura.");
        }
        Factura factura = facturaRepository.findById(enlace.getDocumentoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La factura solicitada ya no esta disponible."));
        if (factura.getEstado() == EstadoFactura.ANULADA) {
            throw new RecursoNoEncontradoException("La factura fue anulada y el enlace dejo de estar disponible.");
        }
        return factura;
    }

    /** Entrega el recibo correspondiente a un token valido y disponible (para el PDF publico). */
    @Transactional
    public Recibo obtenerReciboPublico(String token) {
        EnlacePublicoDocumento enlace = validarEnlace(token);
        if (enlace.getTipoDocumento() != TipoDocumentoPublico.RECIBO) {
            throw new RecursoNoEncontradoException("El enlace no corresponde a un recibo.");
        }
        Recibo recibo = reciboRepository.findById(enlace.getDocumentoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El recibo solicitado ya no esta disponible."));
        if (recibo.getEstado() == EstadoRecibo.ANULADO) {
            throw new RecursoNoEncontradoException("El recibo fue anulado y el enlace dejo de estar disponible.");
        }
        return recibo;
    }

    /**
     * Valida que el token exista y no este vencido. Si ya vencio, lo borra definitivamente
     * (el enlace "dejo de estar disponible") y responde el mensaje correspondiente.
     */
    private EnlacePublicoDocumento validarEnlace(String token) {
        EnlacePublicoDocumento enlace = enlaceRepository.findByToken(token)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Enlace expirado o no disponible. Este enlace dejo de estar disponible."));

        if (enlace.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            enlaceRepository.delete(enlace);
            throw new RecursoNoEncontradoException(
                    "Enlace expirado. Este enlace dejo de estar disponible.");
        }
        return enlace;
    }

    /** Elimina definitivamente todos los enlaces publicos ya vencidos (tarea diaria). */
    @Transactional
    public int eliminarExpirados() {
        long antes = enlaceRepository.count();
        enlaceRepository.deleteByFechaExpiracionBefore(LocalDateTime.now());
        long despues = enlaceRepository.count();
        int eliminados = (int) (antes - despues);
        if (eliminados > 0) {
            log.info("Se eliminaron {} enlace(s) publico(s) de documentos por haber vencido.", eliminados);
        }
        return eliminados;
    }

    private String generarToken() {
        StringBuilder sb = new StringBuilder(LARGO_TOKEN);
        for (int i = 0; i < LARGO_TOKEN; i++) {
            sb.append(ALFABETO_TOKEN.charAt(RANDOM.nextInt(ALFABETO_TOKEN.length())));
        }
        return sb.toString();
    }
}