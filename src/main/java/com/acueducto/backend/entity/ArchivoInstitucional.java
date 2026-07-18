package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.FuenteArchivoInstitucional;
import com.acueducto.backend.entity.enums.TipoArchivoInstitucional;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Historial de logos, firmas y sellos disponibles; solo uno de cada tipo esta activo (10.9 / 10.10). */
@Entity
@Table(name = "archivos_institucionales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchivoInstitucional extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoArchivoInstitucional tipo;

    @Column(nullable = false, length = 200)
    private String nombreArchivo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ruta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private FuenteArchivoInstitucional fuente;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = false;
}
