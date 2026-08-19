package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.Medidor;
import com.acueducto.backend.entity.enums.EstadoMedidor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedidorResponse {
    private Long id;
    private String codigoInterno;
    private String numero;
    private Long asociadoId;
    private String asociadoNombre;
    private LocalDate fechaInstalacion;
    private EstadoMedidor estado;
    private String ubicacion;

    public static MedidorResponse fromEntity(Medidor m) {
        return MedidorResponse.builder()
                .id(m.getId())
                .codigoInterno(m.getCodigoInterno())
                .numero(m.getNumero())
                .asociadoId(m.getAsociado() != null ? m.getAsociado().getId() : null)
                .asociadoNombre(m.getAsociado() != null ? m.getAsociado().getNombres() + " " + m.getAsociado().getApellidos() : null)
                .fechaInstalacion(m.getFechaInstalacion())
                .estado(m.getEstado())
                .ubicacion(m.getUbicacion())
                .build();
    }
}
