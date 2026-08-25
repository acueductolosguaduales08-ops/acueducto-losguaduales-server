package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.TarifaHistorial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarifaHistorialResponse {
    private Long id;
    private BigDecimal valorM3;
    private BigDecimal cargoFijoAdministracion;
    private BigDecimal valorReconexion;
    private BigDecimal valorMultaDefecto;
    private LocalDate fechaVigencia;
    private String observaciones;

    public static TarifaHistorialResponse fromEntity(TarifaHistorial t) {
        return TarifaHistorialResponse.builder()
                .id(t.getId())
                .valorM3(t.getValorM3())
                .cargoFijoAdministracion(t.getCargoFijoAdministracion())
                .valorReconexion(t.getValorReconexion())
                .valorMultaDefecto(t.getValorMultaDefecto())
                .fechaVigencia(t.getFechaVigencia())
                .observaciones(t.getObservaciones())
                .build();
    }
}
