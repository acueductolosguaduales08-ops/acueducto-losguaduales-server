package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Recibo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReciboRepository extends JpaRepository<Recibo, Long> {
    Optional<Recibo> findByNumeroRecibo(String numeroRecibo);
    Optional<Recibo> findByPagoId(Long pagoId);
    Page<Recibo> findByAsociadoId(Long asociadoId, Pageable pageable);
}
