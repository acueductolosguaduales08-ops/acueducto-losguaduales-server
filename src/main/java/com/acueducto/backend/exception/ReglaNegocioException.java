package com.acueducto.backend.exception;

/** Se lanza cuando una operacion viola una regla de negocio del sistema (ver seccion 4 del documento funcional). */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
