package com.acueducto.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncuestaEstadisticasResponse {
    private long totalRespuestas;
    private java.util.Map<String, Long> resumenPorPregunta;
}
