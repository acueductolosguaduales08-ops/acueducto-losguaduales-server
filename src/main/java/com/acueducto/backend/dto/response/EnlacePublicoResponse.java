package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.enums.TipoDocumentoPublico;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Respuesta al generar un enlace publico temporal de descarga de una factura o recibo.
 * El frontend usa {@code publicDownloadUrl} para enviarlo por WhatsApp o compartirlo.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnlacePublicoResponse {
    private Long documentoId;
    private String numeroDocumento;
    private TipoDocumentoPublico tipo;
    private String publicDownloadUrl;
    private LocalDateTime expiresAt;
}