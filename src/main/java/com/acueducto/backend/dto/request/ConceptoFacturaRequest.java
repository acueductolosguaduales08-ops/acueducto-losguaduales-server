package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ConceptoFacturaRequest(
        @NotNull Long facturaId,
        @NotBlank String descripcion,
        @NotNull @Positive BigDecimal valor
) {
}
