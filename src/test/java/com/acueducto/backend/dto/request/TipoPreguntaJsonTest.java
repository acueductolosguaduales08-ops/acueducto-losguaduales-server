package com.acueducto.backend.dto.request;

import com.acueducto.backend.entity.enums.TipoPregunta;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TipoPreguntaJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void debeAceptarValoresLegacyDelFrontendParaOpciones() throws Exception {
        PreguntaEncuestaRequest request = objectMapper.readValue(
                "{\"texto\":\"Pregunta\",\"tipo\":\"OPCIONES\"}",
                PreguntaEncuestaRequest.class
        );

        assertEquals(TipoPregunta.OPCION_UNICA, request.tipo());
    }

    @Test
    void debeAceptarValoresLegacyDelFrontendParaEstrellas() throws Exception {
        PreguntaEncuestaRequest request = objectMapper.readValue(
                "{\"texto\":\"Pregunta\",\"tipo\":\"ESTRELLAS\"}",
                PreguntaEncuestaRequest.class
        );

        assertEquals(TipoPregunta.ESCALA, request.tipo());
    }
}
