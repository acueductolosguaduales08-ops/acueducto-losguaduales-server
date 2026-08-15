package com.acueducto.backend.entity.enums;

/** Modo de exhibicion del hero/banner publico (ver HeroLinkService). */
public enum ModoHero {
    /** Siempre se muestra el HeroLink marcado como principal. */
    UNICO,
    /** Se muestra uno al azar de los registrados, cambiando cada 15 minutos. */
    ALEATORIO_15MIN
}
