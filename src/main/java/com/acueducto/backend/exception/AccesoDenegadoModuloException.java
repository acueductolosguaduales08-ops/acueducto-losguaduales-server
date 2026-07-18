package com.acueducto.backend.exception;

/** Se lanza cuando un usuario intenta acceder a informacion que no le pertenece (p.ej. facturas de otro asociado). */
public class AccesoDenegadoModuloException extends RuntimeException {
    public AccesoDenegadoModuloException(String mensaje) {
        super(mensaje);
    }
}
