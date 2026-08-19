package com.acueducto.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Listado general de asociados (no archivados) ordenado por codigo interno, con el
 * detalle completo de cada uno: medidor, estado del servicio y si tiene cuenta en el
 * sistema. Pensado para que el Tesorero o el Administrador lo descarguen en PDF o lo
 * consulten en HTML, con el logo institucional y SIN firma ni sello.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformeListadoAsociadosResponse {

    private LocalDate fechaGeneracion;
    private long totalAsociados;
    private long asociadosConCuenta;
    private long asociadosSinCuenta;
    private long asociadosActivos;
    private long asociadosSuspendidos;

    private List<AsociadoItem> asociados;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AsociadoItem {
        private String codigoInterno;
        private String nombreCompleto;
        private String documento;
        private String telefonoPrincipal;
        private String correo;
        private String direccion;
        private String estadoServicio;
        private LocalDate fechaAfiliacion;
        private String numeroMedidor;
        private String estadoMedidor;
        private boolean tieneCuenta;
    }
}
