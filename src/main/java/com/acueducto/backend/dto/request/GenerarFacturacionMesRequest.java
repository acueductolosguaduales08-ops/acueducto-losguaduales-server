package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotNull;

/** Ejecuta la "Generacion de Facturacion del Mes" recomendada en 6.14: procesa todas las lecturas pendientes del mes. */
public record GenerarFacturacionMesRequest(
        @NotNull Long mesContableId
) {
}
