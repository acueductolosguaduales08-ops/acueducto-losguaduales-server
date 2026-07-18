package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record MedidorRequest(
        @NotBlank String numero,
        Long asociadoId,
        LocalDate fechaInstalacion,
        String ubicacion,
        String observaciones
) {
}
