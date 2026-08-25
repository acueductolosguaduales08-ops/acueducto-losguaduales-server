package com.acueducto.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tarifas_historial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarifaHistorial extends BaseEntity {

    @Column(name = "valor_m3", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorM3;

    @Column(name = "cargo_fijo_administracion", nullable = false, precision = 12, scale = 2)
    private BigDecimal cargoFijoAdministracion;

    @Column(name = "valor_reconexion", precision = 12, scale = 2)
    private BigDecimal valorReconexion;

    @Column(name = "valor_multa_defecto", precision = 12, scale = 2)
    private BigDecimal valorMultaDefecto;

    @Column(name = "fecha_vigencia", nullable = false)
    private LocalDate fechaVigencia;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}
