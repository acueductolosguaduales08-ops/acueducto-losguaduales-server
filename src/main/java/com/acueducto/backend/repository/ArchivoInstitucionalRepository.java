package com.acueducto.backend.repository;

import com.acueducto.backend.entity.ArchivoInstitucional;
import com.acueducto.backend.entity.enums.TipoArchivoInstitucional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchivoInstitucionalRepository extends JpaRepository<ArchivoInstitucional, Long> {
    List<ArchivoInstitucional> findByTipo(TipoArchivoInstitucional tipo);
}
