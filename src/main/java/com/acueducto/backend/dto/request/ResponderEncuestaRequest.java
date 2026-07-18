package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ResponderEncuestaRequest(
        /** Obligatorio solo cuando quien responde no tiene sesion y el formulario NO es anonimo. */
        String nombre,
        @NotEmpty List<RespuestaPreguntaItem> respuestas
) {
    public record RespuestaPreguntaItem(Long preguntaId, String valor) {
    }
}
