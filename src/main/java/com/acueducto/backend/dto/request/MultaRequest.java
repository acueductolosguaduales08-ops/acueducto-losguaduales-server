package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MultaRequest(
        @NotNull Long asociadoId,
        Long facturaId,
        @NotBlank @Size(max = 200, message = "El motivo no puede superar 200 caracteres") String motivo,
        @NotNull @Positive BigDecimal valor
) {
}
