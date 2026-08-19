package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Solo permite cambiar el valor de la multa; el motivo queda fijo desde su creacion. */
public record EditarValorMultaRequest(
        @NotNull @Positive BigDecimal valor
) {
}
