package com.acueducto.backend.util;

/** Da formato consistente a los numeros consecutivos del sistema (4.9). */
public final class NumeracionUtil {

    private NumeracionUtil() {
    }

    public static String formatearFactura(long numero) {
        return "FAC-" + String.format("%06d", numero);
    }

    public static String formatearRecibo(long numero) {
        return "REC-" + String.format("%06d", numero);
    }

    public static String formatearEntrada(long numero) {
        return "ENT-" + String.format("%06d", numero);
    }

    public static String formatearSalida(long numero) {
        return "SAL-" + String.format("%06d", numero);
    }

    public static String formatearFormulario(long numero) {
        return "FRM-" + String.format("%06d", numero);
    }

    public static String formatearCodigoAsociado(long numero) {
        return "ASO-" + String.format("%05d", numero);
    }
}
