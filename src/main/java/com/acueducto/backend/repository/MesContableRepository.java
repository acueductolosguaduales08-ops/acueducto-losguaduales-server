package com.acueducto.backend.repository;

import com.acueducto.backend.entity.MesContable;
import com.acueducto.backend.entity.enums.EstadoMes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MesContableRepository extends JpaRepository<MesContable, Long> {
    Optional<MesContable> findByNumeroMesAndAnioContableId(Integer numeroMes, Long anioId);
    List<MesContable> findByAnioContableIdOrderByNumeroMes(Long anioId);
    Optional<MesContable> findFirstByEstadoOrderByFechaAperturaDesc(EstadoMes estado);
}
