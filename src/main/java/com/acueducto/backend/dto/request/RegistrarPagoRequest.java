package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RegistrarPagoRequest(
        @NotNull Long facturaId,
        @NotNull @Positive BigDecimal valor,
        @NotNull Long metodoPagoId,
        String observaciones
) {
}
