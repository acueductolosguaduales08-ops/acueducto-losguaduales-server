package com.acueducto.backend.entity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificacionEnumCompatibilityTest {

    @Test
    void shouldAcceptMediaPriority() {
        assertEquals(PrioridadNotificacion.MEDIA, PrioridadNotificacion.valueOf("MEDIA"));
    }

    @Test
    void shouldMapLegacyTipoValues() {
        assertEquals(TipoNotificacion.ASOCIADO, TipoNotificacion.fromValue("PRIVADA"));
        assertEquals(TipoNotificacion.ADMINISTRATIVA, TipoNotificacion.fromValue("INTERNA"));
    }
}
