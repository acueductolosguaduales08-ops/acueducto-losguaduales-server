package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByFacturaId(Long facturaId);
    List<Pago> findByAsociadoId(Long asociadoId);
    List<Pago> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta);
}
