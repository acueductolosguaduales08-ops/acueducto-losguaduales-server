package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.EstadoServicio;
import com.acueducto.backend.entity.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Nucleo del sistema: representa a la persona beneficiaria del servicio de agua.
 * Practicamente todos los demas modulos dependen de esta entidad (5.1).
 */
@Entity
@Table(name = "asociados", uniqueConstraints = {
        @UniqueConstraint(name = "uk_asociado_documento", columnNames = "documento"),
        @UniqueConstraint(name = "uk_asociado_codigo_interno", columnNames = "codigo_interno")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asociado extends BaseEntity {

    @Column(name = "codigo_interno", length = 20)
    private String codigoInterno;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 15)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false, length = 20)
    private String documento;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "telefono_principal", nullable = false, length = 20)
    private String telefonoPrincipal;

    @Column(name = "telefono_alternativo", length = 20)
    private String telefonoAlternativo;

    @Column(length = 150)
    private String correo;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(name = "barrio_vereda", length = 100)
    private String barrioVereda;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    // ---- Informacion del servicio ----

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "estado_servicio", nullable = false, length = 15)
    private EstadoServicio estadoServicio = EstadoServicio.ACTIVO;

    @Column(name = "fecha_afiliacion", nullable = false)
    private LocalDate fechaAfiliacion;

    @Column(name = "fecha_suspension")
    private LocalDate fechaSuspension;

    @Column(name = "motivo_suspension", columnDefinition = "TEXT")
    private String motivoSuspension;

    @Column(name = "fecha_reactivacion")
    private LocalDate fechaReactivacion;

    @Column(name = "observaciones_administrativas", columnDefinition = "TEXT")
    private String observacionesAdministrativas;

    @OneToOne(mappedBy = "asociado", fetch = FetchType.LAZY)
    private Medidor medidor;

    /** Baja logica: un asociado con historial (facturas, pagos) nunca se elimina fisicamente (5.8). */
    @Builder.Default
    @Column(nullable = false)
    private boolean archivado = false;
}
