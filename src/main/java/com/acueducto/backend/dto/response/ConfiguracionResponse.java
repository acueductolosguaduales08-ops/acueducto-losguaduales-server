package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.Configuracion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionResponse {
    private Long id;
    private String nombreAcueducto;
    private String nit;
    private String direccion;
    private String telefonoPrincipal;
    private String correo;
    private String municipio;
    private String departamento;
    private String banco;
    private String tipoCuenta;
    private String numeroCuenta;
    private String titularCuenta;
    private BigDecimal valorM3;
    private BigDecimal cargoFijoAdministracion;
    private BigDecimal valorReconexion;
    private BigDecimal valorMultaDefecto;
    private Integer diasPlazoPago;
    private String notasFactura;
    private String logoActivo;
    private String firmaActiva;
    private String selloActivo;

    public static ConfiguracionResponse fromEntity(Configuracion c) {
        return ConfiguracionResponse.builder()
                .id(c.getId()).nombreAcueducto(c.getNombreAcueducto()).nit(c.getNit())
                .direccion(c.getDireccion()).telefonoPrincipal(c.getTelefonoPrincipal())
                .correo(c.getCorreo()).municipio(c.getMunicipio()).departamento(c.getDepartamento())
                .banco(c.getBanco()).tipoCuenta(c.getTipoCuenta()).numeroCuenta(c.getNumeroCuenta())
                .titularCuenta(c.getTitularCuenta()).valorM3(c.getValorM3())
                .cargoFijoAdministracion(c.getCargoFijoAdministracion()).valorReconexion(c.getValorReconexion())
                .valorMultaDefecto(c.getValorMultaDefecto()).diasPlazoPago(c.getDiasPlazoPago())
                .notasFactura(c.getNotasFactura()).logoActivo(c.getLogoActivo())
                .firmaActiva(c.getFirmaActiva()).selloActivo(c.getSelloActivo())
                .build();
    }
}
