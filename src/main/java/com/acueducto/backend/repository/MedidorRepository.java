package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Medidor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedidorRepository extends JpaRepository<Medidor, Long> {
    Optional<Medidor> findByNumero(String numero);
    boolean existsByNumero(String numero);
    Optional<Medidor> findByAsociadoId(Long asociadoId);
}
