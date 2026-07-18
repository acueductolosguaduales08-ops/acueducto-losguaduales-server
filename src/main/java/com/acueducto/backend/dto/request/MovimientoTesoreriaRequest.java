package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Registra ingresos extraordinarios o gastos (Modulo 8): tipo se decide segun el endpoint invocado. */
public record MovimientoTesoreriaRequest(
        @NotNull @Positive BigDecimal valor,
        @NotNull Long metodoPagoId,
        @NotBlank @Size(max = 200, message = "El concepto no puede superar 200 caracteres") String concepto,
        @Size(max = 60, message = "La categoria no puede superar 60 caracteres") String categoria,
        String observaciones,
        Long asociadoId,
        @NotNull Long mesContableId,
        String comprobanteUrl
) {
}
