package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record AnioContableRequest(@NotNull Integer anio) {
}
