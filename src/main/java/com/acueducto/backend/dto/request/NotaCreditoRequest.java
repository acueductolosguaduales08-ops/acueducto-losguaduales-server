package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record NotaCreditoRequest(
    @NotNull(message = "El asociado es obligatorio")
    Long asociadoId,

    Long facturaId,

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
    String motivo,

    @NotNull(message = "El valor es obligatorio")
    @Positive(message = "El valor debe ser mayor a cero")
    BigDecimal valor,

    String observaciones
) {}
