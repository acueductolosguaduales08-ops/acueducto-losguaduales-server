package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.Asociado;
import com.acueducto.backend.entity.enums.EstadoServicio;
import com.acueducto.backend.entity.enums.TipoDocumento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsociadoResponse {
    private Long id;
    private String codigoInterno;
    private TipoDocumento tipoDocumento;
    private String documento;
    private String nombres;
    private String apellidos;
    private String telefonoPrincipal;
    private String telefonoAlternativo;
    private String correo;
    private String direccion;
    private String barrioVereda;
    private EstadoServicio estadoServicio;
    private LocalDate fechaAfiliacion;
    private String numeroMedidor;
    private boolean archivado;

    public static AsociadoResponse fromEntity(Asociado a) {
        return AsociadoResponse.builder()
                .id(a.getId())
                .codigoInterno(a.getCodigoInterno())
                .tipoDocumento(a.getTipoDocumento())
                .documento(a.getDocumento())
                .nombres(a.getNombres())
                .apellidos(a.getApellidos())
                .telefonoPrincipal(a.getTelefonoPrincipal())
                .telefonoAlternativo(a.getTelefonoAlternativo())
                .correo(a.getCorreo())
                .direccion(a.getDireccion())
                .barrioVereda(a.getBarrioVereda())
                .estadoServicio(a.getEstadoServicio())
                .fechaAfiliacion(a.getFechaAfiliacion())
                .numeroMedidor(a.getMedidor() != null ? a.getMedidor().getNumero() : null)
                .archivado(a.isArchivado())
                .build();
    }
}
