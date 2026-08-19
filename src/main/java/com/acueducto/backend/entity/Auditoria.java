package com.acueducto.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Registro inmutable de acciones importantes del sistema (2.13 / 4.4). */
@Entity
@Table(name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 60)
    private String usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(nullable = false, length = 60)
    private String modulo;

    @Column(name = "registro_afectado", length = 60)
    private String registroAfectado;

    @Column(length = 45)
    private String ip;

    @Column(columnDefinition = "TEXT")
    private String observaciones;
}
