package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/** El link va en el body (no en la URL ni un query param) para aceptar links largos y con caracteres especiales sin problema. */
public record HeroLinkRequest(
        @NotBlank String link
) {
}
