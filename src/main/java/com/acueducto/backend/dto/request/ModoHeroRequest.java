package com.acueducto.backend.dto.request;

import com.acueducto.backend.entity.enums.ModoHero;
import jakarta.validation.constraints.NotNull;

public record ModoHeroRequest(
        @NotNull ModoHero modo
) {
}
