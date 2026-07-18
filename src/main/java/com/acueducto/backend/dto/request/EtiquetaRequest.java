package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EtiquetaRequest(@NotBlank String nombre, String color) {
}
