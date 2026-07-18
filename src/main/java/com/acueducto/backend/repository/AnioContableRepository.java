package com.acueducto.backend.repository;

import com.acueducto.backend.entity.AnioContable;
import com.acueducto.backend.entity.enums.EstadoAnio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnioContableRepository extends JpaRepository<AnioContable, Long> {
    Optional<AnioContable> findByAnio(Integer anio);
    Optional<AnioContable> findByEstado(EstadoAnio estado);
    boolean existsByAnio(Integer anio);
}
