package com.acueducto.backend.repository;

import com.acueducto.backend.entity.MovimientoBancario;
import com.acueducto.backend.entity.enums.EstadoConciliacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoBancarioRepository extends JpaRepository<MovimientoBancario, Long> {
    List<MovimientoBancario> findByEstadoOrderByFechaTransaccionDesc(EstadoConciliacion estado);
    List<MovimientoBancario> findAllByOrderByFechaTransaccionDesc();
    long countByEstado(EstadoConciliacion estado);
}
