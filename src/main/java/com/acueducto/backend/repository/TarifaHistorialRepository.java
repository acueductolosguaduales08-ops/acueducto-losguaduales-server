package com.acueducto.backend.repository;

import com.acueducto.backend.entity.TarifaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarifaHistorialRepository extends JpaRepository<TarifaHistorial, Long> {
    List<TarifaHistorial> findAllByOrderByFechaVigenciaDesc();
}
