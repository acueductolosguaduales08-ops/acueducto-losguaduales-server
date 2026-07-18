package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReporteCiudadanoRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombre,
        @NotBlank(message = "El mensaje es obligatorio") String mensaje,
        @Size(max = 150, message = "El dato de contacto es demasiado largo") String contacto
) {
}
