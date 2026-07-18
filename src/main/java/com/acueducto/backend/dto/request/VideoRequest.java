package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VideoRequest(
        @NotBlank @Size(max = 200, message = "El titulo no puede superar 200 caracteres") String titulo,
        String descripcion,
        @NotBlank String urlVideo
) {
}
