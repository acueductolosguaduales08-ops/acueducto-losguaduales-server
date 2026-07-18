package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.EstadoMedidor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "medidores", uniqueConstraints = {
        @UniqueConstraint(name = "uk_medidor_numero", columnNames = "numero"),
        @UniqueConstraint(name = "uk_medidor_codigo_interno", columnNames = "codigo_interno")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medidor extends BaseEntity {

    @Column(name = "codigo_interno", length = 20)
    private String codigoInterno;

    @Column(nullable = false, length = 30)
    private String numero;

    /** Un medidor solo puede estar asignado a un asociado a la vez (6.4 / 6.14). */
    @OneToOne
    @JoinColumn(name = "asociado_id", unique = true)
    private Asociado asociado;

    @Column(name = "fecha_instalacion")
    private LocalDate fechaInstalacion;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private EstadoMedidor estado = EstadoMedidor.ACTIVO;

    @Column(length = 150)
    private String ubicacion;

    @Column(columnDefinition = "TEXT")
    private String observaciones;
}
