package com.acueducto.backend.dto.request;

import com.acueducto.backend.entity.enums.TipoPregunta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PreguntaEncuestaRequest(
        @NotBlank String texto,
        @NotNull TipoPregunta tipo,
        boolean obligatoria,
        Integer orden,
        List<String> opciones
) {
}
