package com.acueducto.backend.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum TipoNotificacion {
    PUBLICA,
    ASOCIADO,
    ADMINISTRATIVA;

    @JsonCreator
    public static TipoNotificacion fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PUBLICA" -> PUBLICA;
            case "ASOCIADO" -> ASOCIADO;
            case "ADMINISTRATIVA" -> ADMINISTRATIVA;
            case "PRIVADA" -> ASOCIADO;
            case "INTERNA" -> ADMINISTRATIVA;
            default -> throw new IllegalArgumentException("Tipo de notificación no soportado: " + value);
        };
    }
}
