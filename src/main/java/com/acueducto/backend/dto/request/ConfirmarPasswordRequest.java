package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Reconfirmacion de contrasena para operaciones sensibles: listar cuentas (8) y eliminacion definitiva (5). */
public record ConfirmarPasswordRequest(
        @NotBlank String password
) {
}
