package com.acueducto.backend.dto.request;

import com.acueducto.backend.entity.enums.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AsociadoRequest(
        @NotNull TipoDocumento tipoDocumento,
        @NotBlank @Size(max = 20) String documento,
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        LocalDate fechaNacimiento,
        @NotBlank @Size(max = 20) String telefonoPrincipal,
        @Size(max = 20) String telefonoAlternativo,
        @Email @Size(max = 150) String correo,
        @NotBlank @Size(max = 200) String direccion,
        @Size(max = 100) String barrioVereda,
        String observaciones,
        @NotBlank(message = "El numero de medidor es obligatorio para crear el asociado") String numeroMedidor,
        LocalDate fechaAfiliacion
) {
}
