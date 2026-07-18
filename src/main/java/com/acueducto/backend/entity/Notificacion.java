package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.EstadoNotificacion;
import com.acueducto.backend.entity.enums.PrioridadNotificacion;
import com.acueducto.backend.entity.enums.TipoNotificacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(name = "descripcion_corta", length = 500)
    private String descripcionCorta;

    @Column(name = "contenido_completo", columnDefinition = "TEXT")
    private String contenidoCompleto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoNotificacion tipo;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 10)
    private PrioridadNotificacion prioridad = PrioridadNotificacion.NORMAL;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 15)
    private EstadoNotificacion estado = EstadoNotificacion.ACTIVA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    /** Destinatario especifico (opcional). Si es nulo y el tipo es ASOCIADO, va para todos los asociados. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id")
    private Usuario destinatario;

    /** Enlace opcional hacia un recurso relacionado: factura, recibo, formulario, publicacion. */
    @Column(name = "enlace_url", columnDefinition = "TEXT")
    private String enlaceUrl;
}
