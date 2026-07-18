package com.acueducto.backend.repository;

import com.acueducto.backend.entity.ReporteCiudadano;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReporteCiudadanoRepository extends JpaRepository<ReporteCiudadano, Long> {
    Page<ReporteCiudadano> findAllByOrderByFechaCreacionDesc(Pageable pageable);
    List<ReporteCiudadano> findByFechaEliminacionBefore(LocalDateTime momento);
}
