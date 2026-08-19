package com.acueducto.backend.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoPregunta {
    TEXTO_CORTO,
    TEXTO_LARGO,
    OPCION_UNICA,
    OPCION_MULTIPLE,
    ESCALA,
    SI_NO;

    @JsonCreator
    public static TipoPregunta fromValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "OPCIONES" -> OPCION_UNICA;
            case "ESTRELLAS" -> ESCALA;
            case "TEXTO_CORTO" -> TEXTO_CORTO;
            case "TEXTO_LARGO" -> TEXTO_LARGO;
            case "OPCION_UNICA" -> OPCION_UNICA;
            case "OPCION_MULTIPLE" -> OPCION_MULTIPLE;
            case "ESCALA" -> ESCALA;
            case "SI_NO" -> SI_NO;
            default -> throw new IllegalArgumentException("Tipo de pregunta no soportado: " + value);
        };
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
