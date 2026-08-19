package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.Notificacion;
import com.acueducto.backend.entity.enums.EstadoNotificacion;
import com.acueducto.backend.entity.enums.PrioridadNotificacion;
import com.acueducto.backend.entity.enums.TipoNotificacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionResponse {
    private Long id;
    private String titulo;
    private String descripcionCorta;
    private String contenidoCompleto;
    private TipoNotificacion tipo;
    private PrioridadNotificacion prioridad;
    private EstadoNotificacion estado;
    private LocalDateTime fechaPublicacion;
    private LocalDateTime fechaVencimiento;
    private String enlaceUrl;
    private boolean leida;

    public static NotificacionResponse fromEntity(Notificacion n, boolean leida) {
        return NotificacionResponse.builder()
                .id(n.getId()).titulo(n.getTitulo()).descripcionCorta(n.getDescripcionCorta())
                .contenidoCompleto(n.getContenidoCompleto()).tipo(n.getTipo()).prioridad(n.getPrioridad())
                .estado(n.getEstado()).fechaPublicacion(n.getFechaPublicacion())
                .fechaVencimiento(n.getFechaVencimiento()).enlaceUrl(n.getEnlaceUrl()).leida(leida)
                .build();
    }
}
