package com.acueducto.backend.dto.request;

import com.acueducto.backend.entity.enums.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Datos que un asociado puede editar de su propio perfil. No incluye medidor, nombres, apellidos ni fecha de afiliacion. */
public record ActualizarDatosAsociadoRequest(
        @NotNull TipoDocumento tipoDocumento,
        @NotBlank String documento,
        LocalDate fechaNacimiento,
        String telefonoPrincipal,
        String telefonoAlternativo,
        @Email String correo,
        @NotBlank String direccion,
        String barrioVereda
) {
}
