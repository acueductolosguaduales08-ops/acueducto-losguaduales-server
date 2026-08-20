package com.acueducto.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Informa cual sera la lectura anterior que el backend usara por defecto al registrar una lectura
 * nueva para un medidor: la lectura actual de su ultimo registro, o 0 si aun no tiene historial.
 * Permite al usuario ver/confirmar ese valor antes de guardar.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturaAnteriorPorDefectoResponse {
    private Long medidorId;
    private String numeroMedidor;
    private Long asociadoId;
    private String asociadoNombre;
    private Integer lecturaAnteriorPorDefecto;
    private Long ultimaLecturaId;
    private LocalDate ultimaFechaLectura;
    private boolean hayRegistroPrevio;
}