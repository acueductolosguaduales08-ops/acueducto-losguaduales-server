package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MesContableRequest(
        @NotNull Long anioContableId,
        @NotNull @Min(1) @Max(12) Integer numeroMes
) {
}
