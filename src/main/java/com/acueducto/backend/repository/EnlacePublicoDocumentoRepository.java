package com.acueducto.backend.repository;

import com.acueducto.backend.entity.EnlacePublicoDocumento;
import com.acueducto.backend.entity.enums.TipoDocumentoPublico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EnlacePublicoDocumentoRepository extends JpaRepository<EnlacePublicoDocumento, Long> {
    Optional<EnlacePublicoDocumento> findByToken(String token);
    void deleteByTipoDocumentoAndDocumentoId(TipoDocumentoPublico tipoDocumento, Long documentoId);
    void deleteByFechaExpiracionBefore(LocalDateTime fecha);
}