package com.acueducto.backend.dto.request;

import com.acueducto.backend.entity.enums.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres") String password,
        @NotBlank @Pattern(regexp = "^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$|^[\\d\\s\\-+()]{7,15}$",
                message = "Debe ser un correo electronico o un numero de telefono valido") String contacto,
        @NotNull Rol rol,
        Long asociadoId
) {
}
