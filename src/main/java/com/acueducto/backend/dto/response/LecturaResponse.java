package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.Lectura;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturaResponse {
    private Long id;
    private Long asociadoId;
    private String asociadoNombre;
    private Long medidorId;
    private String numeroMedidor;
    private Long mesContableId;
    private LocalDate fechaLectura;
    private Integer lecturaAnterior;
    private Integer lecturaActual;
    private Integer consumoM3;
    private boolean facturaGenerada;

    public static LecturaResponse fromEntity(Lectura l) {
        return LecturaResponse.builder()
                .id(l.getId())
                .asociadoId(l.getAsociado().getId())
                .asociadoNombre(l.getAsociado().getNombres() + " " + l.getAsociado().getApellidos())
                .medidorId(l.getMedidor().getId())
                .numeroMedidor(l.getMedidor().getNumero())
                .mesContableId(l.getMesContable().getId())
                .fechaLectura(l.getFechaLectura())
                .lecturaAnterior(l.getLecturaAnterior())
                .lecturaActual(l.getLecturaActual())
                .consumoM3(l.getConsumoM3())
                .facturaGenerada(l.isFacturaGenerada())
                .build();
    }
}
