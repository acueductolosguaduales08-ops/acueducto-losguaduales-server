package com.acueducto.backend.dto.request;

import com.acueducto.backend.entity.enums.EstadoServicio;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoServicioRequest(
        @NotNull EstadoServicio estado,
        String motivo
) {
}
