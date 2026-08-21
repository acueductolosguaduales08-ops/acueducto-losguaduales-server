package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Solicitud para activar o bloquear una cuenta de usuario. Requiere la contrasena del administrador autenticado. */
public record CambiarEstadoCuentaRequest(
        @NotBlank String password,
        String motivo
) {
}
