package com.acueducto.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Reporte ciudadano de fuga o queja/reclamo, enviado de forma publica y sin inicio de sesion.
 * Es un modulo independiente y temporal: cada registro se elimina automaticamente 8 dias
 * despues de haber sido creado (ver TareasProgramadasService.eliminarReportesCiudadanosVencidos).
 */
@Entity
@Table(name = "reportes_ciudadanos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteCiudadano extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    /** Dato de contacto opcional: telefono, celular o correo. Texto libre. */
    @Column(length = 150)
    private String contacto;

    /** Fecha en la que el reporte se elimina automaticamente (fecha de creacion + 8 dias). */
    @Column(name = "fecha_eliminacion", nullable = false)
    private LocalDateTime fechaEliminacion;
}
