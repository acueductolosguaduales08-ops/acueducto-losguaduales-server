package com.acueducto.backend.repository;

import com.acueducto.backend.entity.HeroLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HeroLinkRepository extends JpaRepository<HeroLink, Long> {
    Optional<HeroLink> findByPrincipalTrue();
}
