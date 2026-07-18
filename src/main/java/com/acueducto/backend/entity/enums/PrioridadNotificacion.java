package com.acueducto.backend.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum PrioridadNotificacion {
    BAJA,
    NORMAL,
    MEDIA,
    ALTA,
    URGENTE;

    @JsonCreator
    public static PrioridadNotificacion fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BAJA" -> BAJA;
            case "NORMAL" -> NORMAL;
            case "MEDIA" -> MEDIA;
            case "ALTA" -> ALTA;
            case "URGENTE" -> URGENTE;
            default -> throw new IllegalArgumentException("Prioridad de notificación no soportada: " + value);
        };
    }
}
