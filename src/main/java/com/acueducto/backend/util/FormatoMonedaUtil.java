package com.acueducto.backend.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Da formato colombiano a los valores monetarios de facturas y recibos (7.6 / 8.12):
 * punto como separador de miles y sin decimales cuando el valor es entero. */
public final class FormatoMonedaUtil {

    private FormatoMonedaUtil() {
    }

    public static String formatear(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.getDefault());
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        boolean esEntero = valor.stripTrailingZeros().scale() <= 0;
        DecimalFormat formato = new DecimalFormat(esEntero ? "#,##0" : "#,##0.00", simbolos);
        return formato.format(valor);
    }

    public static String formatearConSimbolo(BigDecimal valor) {
        return "$" + formatear(valor);
    }
}