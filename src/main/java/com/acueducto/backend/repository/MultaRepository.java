package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Multa;
import com.acueducto.backend.entity.enums.EstadoMulta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MultaRepository extends JpaRepository<Multa, Long> {
    List<Multa> findByAsociadoId(Long asociadoId);
    List<Multa> findByAsociadoIdAndEstado(Long asociadoId, EstadoMulta estado);
}
