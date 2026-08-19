package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

public record EncuestaRequest(
        @NotBlank String titulo,
        String descripcion,
        boolean publico,
        boolean requiereAutenticacion,
        boolean respuestaUnica,
        boolean respuestasAnonimas,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        List<PreguntaEncuestaRequest> preguntas
) {
}
