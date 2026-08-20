package com.acueducto.backend.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatoMonedaUtilTest {

    @Test
    void formatearConSimboloEnteroConPuntoDeMiles() {
        assertEquals("$30.000", FormatoMonedaUtil.formatearConSimbolo(new BigDecimal("30000.00")));
        assertEquals("$288.000", FormatoMonedaUtil.formatearConSimbolo(new BigDecimal("288000.00")));
        assertEquals("$1.500.000", FormatoMonedaUtil.formatearConSimbolo(new BigDecimal("1500000.00")));
        assertEquals("$8.000", FormatoMonedaUtil.formatearConSimbolo(new BigDecimal("8000.00")));
    }

    @Test
    void formatearConSimboloValorCero() {
        assertEquals("$0", FormatoMonedaUtil.formatearConSimbolo(BigDecimal.ZERO));
    }

    @Test
    void formatearConSimboloValorConDecimales() {
        assertEquals("$1.234,50", FormatoMonedaUtil.formatearConSimbolo(new BigDecimal("1234.50")));
    }

    @Test
    void formatearNullDevuelveVacio() {
        assertEquals("", FormatoMonedaUtil.formatear(null));
    }
}