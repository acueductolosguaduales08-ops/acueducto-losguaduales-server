package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ConfiguracionRequest(
        @NotBlank String nombreAcueducto,
        String nit,
        String direccion,
        String telefonoPrincipal,
        String correo,
        String municipio,
        String departamento,
        String banco,
        String tipoCuenta,
        String numeroCuenta,
        String titularCuenta,
        @NotNull @Positive BigDecimal valorM3,
        @NotNull @Positive BigDecimal cargoFijoAdministracion,
        BigDecimal valorReconexion,
        BigDecimal valorMultaDefecto,
        Integer diasPlazoPago,
        String notasFactura
) {
}
