package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record LecturaRequest(
        @NotNull Long medidorId,
        @NotNull Long mesContableId,
        @NotNull LocalDate fechaLectura,
        @NotNull @PositiveOrZero Integer lecturaActual,
        String observaciones
) {
}
