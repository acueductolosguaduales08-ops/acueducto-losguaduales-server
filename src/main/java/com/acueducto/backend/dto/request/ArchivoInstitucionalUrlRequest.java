package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ArchivoInstitucionalUrlRequest(
        @NotBlank String url,
        String nombreArchivo
) {
}
