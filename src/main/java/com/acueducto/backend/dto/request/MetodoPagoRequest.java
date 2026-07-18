package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MetodoPagoRequest(@NotBlank String nombre) {
}
