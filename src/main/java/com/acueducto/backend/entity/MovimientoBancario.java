package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.EstadoConciliacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "movimientos_bancarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoBancario extends BaseEntity {

    @Column(name = "fecha_transaccion", nullable = false)
    private LocalDate fechaTransaccion;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(length = 50)
    private String numeroReferencia;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private EstadoConciliacion estado = EstadoConciliacion.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_tesoreria_id")
    private MovimientoTesoreria movimientoTesoreria;

    @Column(name = "archivo_origen", length = 200)
    private String archivoOrigen;

    @Column(columnDefinition = "TEXT")
    private String observaciones;
}
