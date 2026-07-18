package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CategoriaRequest(@NotBlank String nombre) {
}
