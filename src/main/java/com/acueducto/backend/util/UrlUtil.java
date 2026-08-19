package com.acueducto.backend.util;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Validacion y normalizacion compartida para todo campo de tipo URL/link: logo, firma y sello
 * institucional, link de hero, imagen de reportes ciudadanos, etc. En vez de rechazar o borrar
 * caracteres especiales (comillas, espacios, acentos, parametros firmados, etc.), los codifica
 * correctamente para que la URL sea valida sin importar de donde se copio. No impone limite de
 * longitud: se espera que quien la use guarde el resultado en una columna TEXT (por eso una URL
 * larga, por ejemplo firmada tipo S3, no rompe el guardado).
 *
 * Extraida de ConfiguracionService para poder reutilizarla en otros modulos sin duplicar codigo.
 */
public final class UrlUtil {

    private static final Pattern CARACTER_INSEGURO_URL =
            Pattern.compile("[^A-Za-z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]");

    private UrlUtil() {
    }

    public static String normalizar(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("La URL no puede estar vacia");
        }

        String limpia = url.trim();
        while (limpia.length() >= 2 && esParDeComillas(limpia.charAt(0), limpia.charAt(limpia.length() - 1))) {
            limpia = limpia.substring(1, limpia.length() - 1).trim();
        }
        limpia = limpia.replace("\\", "/");

        if (!limpia.matches("(?i)^https?://.+")) {
            throw new IllegalArgumentException("La URL debe empezar por http:// o https://");
        }

        return codificarCaracteresEspeciales(limpia);
    }

    private static boolean esParDeComillas(char inicio, char fin) {
        return (inicio == '"' && fin == '"')
                || (inicio == '\'' && fin == '\'')
                || (inicio == '\u201c' && fin == '\u201d')
                || (inicio == '\u2018' && fin == '\u2019')
                || (inicio == '<' && fin == '>');
    }

    /** Codifica en porcentaje cualquier caracter no valido en una URL (comillas, espacios, acentos, etc.). */
    private static String codificarCaracteresEspeciales(String url) {
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (CARACTER_INSEGURO_URL.matcher(String.valueOf(c)).matches()) {
                for (byte b : String.valueOf(c).getBytes(StandardCharsets.UTF_8)) {
                    resultado.append(String.format("%%%02X", b));
                }
            } else {
                resultado.append(c);
            }
        }
        return resultado.toString();
    }
}
