package com.acueducto.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Mensaje de chat. Exclusivamente texto y emojis (sin imagenes, archivos, audios ni
 * videos). Solo texto/emojis: no se admite contenido multimedia.
 *
 * La fecha de creacion/modificacion proviene de BaseEntity (auditoria JPA).
 * Los mensajes se retienen en PostgreSQL un maximo de 8 dias y luego se eliminan
 * automaticamente (el historial completo vive en IndexedDB del lado del cliente).
 */
@Entity
@Table(name = "mensajes", indexes = {
        @Index(name = "idx_mensaje_conversacion", columnList = "conversacion_id"),
        @Index(name = "idx_mensaje_conversacion_id", columnList = "conversacion_id,id"),
        @Index(name = "idx_mensaje_fecha_creacion", columnList = "fecha_creacion")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private Conversacion conversacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remitente_id", nullable = false)
    private Usuario remitente;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Builder.Default
    @Column(nullable = false)
    private boolean editado = false;

    /**
     * Estado de lectura visto desde el destinatario: true cuando el otro participante
     * (no remitente) ya lo leyo. El remitente siempre considera leidos sus propios mensajes.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean leido = false;

    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;
}