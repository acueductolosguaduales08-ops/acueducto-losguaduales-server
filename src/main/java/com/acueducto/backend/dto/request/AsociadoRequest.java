package com.acueducto.backend.dto.request;

import com.acueducto.backend.entity.enums.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AsociadoRequest(
        @NotNull TipoDocumento tipoDocumento,
        @NotBlank String documento,
        @NotBlank String nombres,
        @NotBlank String apellidos,
        LocalDate fechaNacimiento,
        @NotBlank String telefonoPrincipal,
        String telefonoAlternativo,
        @Email String correo,
        @NotBlank String direccion,
        String barrioVereda,
        String observaciones,
        @NotBlank(message = "El numero de medidor es obligatorio para crear el asociado") String numeroMedidor,
        LocalDate fechaAfiliacion
) {
}
