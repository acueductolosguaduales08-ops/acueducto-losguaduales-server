package com.acueducto.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un link de hero/banner registrado (imagen o video para el portal publico o la app). Puede
 * haber varios registrados; segun el modo de Configuracion.modoHero se muestra siempre el
 * marcado como "principal" (modo UNICO) o uno al azar que rota cada 15 minutos (modo
 * ALEATORIO_15MIN). Ver HeroLinkService.
 */
@Entity
@Table(name = "hero_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeroLink extends BaseEntity {

    /**
     * TEXT y sin limite de longitud, normalizada con UrlUtil: acepta links largos y con
     * caracteres especiales sin romper el guardado (igual que logo/firma/sello).
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String link;

    /** Solo tiene efecto en modo UNICO. Como mucho un HeroLink puede ser principal a la vez. */
    @Builder.Default
    @Column(nullable = false)
    private boolean principal = false;
}
