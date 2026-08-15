package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.enums.ModoHero;

/** Lo que se muestra ahora mismo: el link vigente segun el modo activo. Puede venir con link null si aun no hay ninguno registrado. */
public record HeroActualResponse(
        String link,
        ModoHero modo
) {
}
