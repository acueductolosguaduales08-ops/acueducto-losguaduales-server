package com.acueducto.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Diagnostico previo de una eliminacion definitiva dentro del modulo "Gestion de datos
 * importantes". Informa al frontend que se va a borrar (y en cascada) antes de que el
 * Administrador confirme con su contrasena. Cuando algo no se puede borrar, se indica en
 * motivosBloqueo con la explicacion.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionBorradoResponse {
    private String tipo;
    private Long id;
    private boolean borrable;
    private String referencia;
    private java.util.List<String> motivosBloqueo;
    private Map<String, Long> cascada;
    private String mensaje;

    public static VerificacionBorradoResponse bloqueda(String tipo, Long id, String referencia, java.util.List<String> motivos) {
        return VerificacionBorradoResponse.builder()
                .tipo(tipo)
                .id(id)
                .referencia(referencia)
                .borrable(false)
                .motivosBloqueo(motivos)
                .cascada(Map.of())
                .build();
    }

    public static VerificacionBorradoResponse borrable(String tipo, Long id, String referencia, String mensaje) {
        return VerificacionBorradoResponse.builder()
                .tipo(tipo)
                .id(id)
                .referencia(referencia)
                .borrable(true)
                .motivosBloqueo(java.util.List.of())
                .cascada(new LinkedHashMap<>())
                .mensaje(mensaje)
                .build();
    }
}