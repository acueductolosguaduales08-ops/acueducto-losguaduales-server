package com.acueducto.backend.service;

import com.acueducto.backend.entity.ArchivoInstitucional;
import com.acueducto.backend.entity.enums.FuenteArchivoInstitucional;
import com.acueducto.backend.entity.enums.TipoArchivoInstitucional;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.repository.ArchivoInstitucionalRepository;
import com.acueducto.backend.repository.ConfiguracionRepository;
import com.acueducto.backend.repository.MetodoPagoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionServiceTest {

    @Mock
    private ConfiguracionRepository configuracionRepository;

    @Mock
    private MetodoPagoRepository metodoPagoRepository;

    @Mock
    private ArchivoInstitucionalRepository archivoInstitucionalRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ConfiguracionService configuracionService;

    // Los archivos institucionales (logo, firma, sello) solo se manejan por URL, igual que las
    // imagenes de publicaciones: no hay subida ni almacenamiento local. Antes el bug era que
    // una URL larga, con espacios o comillas rompia el guardado; ahora se normaliza (codificando
    // los caracteres especiales) y se persiste sin limite de longitud.
    @Test
    void deberiaRegistrarArchivoInstitucionalDesdeUrl() {
        when(archivoInstitucionalRepository.save(any(ArchivoInstitucional.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArchivoInstitucional resultado = configuracionService.subirArchivoInstitucionalDesdeUrl(
                TipoArchivoInstitucional.LOGO,
                "https://example.com/logo.png",
                "logo.png"
        );

        assertNotNull(resultado);
        assertEquals(TipoArchivoInstitucional.LOGO, resultado.getTipo());
        assertEquals(FuenteArchivoInstitucional.URL, resultado.getFuente());
        assertEquals("https://example.com/logo.png", resultado.getRuta());
        assertEquals("logo.png", resultado.getNombreArchivo());
    }

    @Test
    void deberiaAceptarUrlLargaConEspaciosYComillas() {
        when(archivoInstitucionalRepository.save(any(ArchivoInstitucional.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String nombreLargoConEspacios = "logo final \"version 2\".png";
        String urlConFirmaLarga = "https://bucket.s3.amazonaws.com/institucional/" + nombreLargoConEspacios
                + "?X-Amz-Signature=" + "a".repeat(250) + "&X-Amz-Expires=3600";

        ArchivoInstitucional resultado = configuracionService.subirArchivoInstitucionalDesdeUrl(
                TipoArchivoInstitucional.LOGO,
                "  \"" + urlConFirmaLarga + "\"  ",
                null
        );

        assertNotNull(resultado);
        assertEquals(FuenteArchivoInstitucional.URL, resultado.getFuente());
        // La comilla envolvente se quita, pero la URL en si (con su firma larga) se conserva completa.
        assertTrue(resultado.getRuta().startsWith("https://bucket.s3.amazonaws.com/"));
        assertTrue(resultado.getRuta().contains("X-Amz-Signature="));
        // El nombre de archivo derivado nunca supera el limite de la columna (200), sin importar
        // cuan largo/raro sea el segmento final de la URL.
        assertTrue(resultado.getNombreArchivo().length() <= 200);
    }

    @Test
    void deberiaRechazarUrlSinEsquemaHttp() {
        assertThrows(IllegalArgumentException.class, () ->
                configuracionService.subirArchivoInstitucionalDesdeUrl(
                        TipoArchivoInstitucional.LOGO,
                        "ftp://example.com/logo.png",
                        "logo.png"
                )
        );
    }

    @Test
    void deberiaEliminarArchivoInstitucionalRegistrado() {
        ArchivoInstitucional existente = ArchivoInstitucional.builder()
                .tipo(TipoArchivoInstitucional.LOGO)
                .nombreArchivo("logo.png")
                .ruta("https://example.com/logo.png")
                .fuente(FuenteArchivoInstitucional.URL)
                .activo(false)
                .build();
        existente.setId(1L);
        when(archivoInstitucionalRepository.findById(1L)).thenReturn(Optional.of(existente));

        configuracionService.eliminarArchivoInstitucional(1L);

        verify(archivoInstitucionalRepository).delete(existente);
    }

    @Test
    void deberiaFallarAlEliminarArchivoQueNoExiste() {
        when(archivoInstitucionalRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () ->
                configuracionService.eliminarArchivoInstitucional(99L));
    }

    // resolverImagenParaDocumento es lo que arma el <img> (src + ancho/alto exactos) para el
    // HTML/PDF de facturas, recibos e informes. Como ahora todo es por URL, nunca debe intentar
    // leer nada de disco.
    @Test
    void resolverImagenParaDocumentoNuncaDebeTocarStorage() {
        // Se usa una URL que no resuelve (dominio .invalid, reservado y siempre no-existente)
        // para no depender de red real en las pruebas; lo que interesa verificar es que, si la
        // URL no responde, se devuelve null (el documento se genera sin el logo) en vez de
        // lanzar un error o intentar leer una ruta local.
        ConfiguracionService.ImagenDocumento resultado = configuracionService.resolverImagenParaDocumento(
                "https://logo.invalid/no-existe.png", TipoArchivoInstitucional.LOGO, 90, 90);
        assertNull(resultado);
    }

    @Test
    void resolverImagenParaDocumentoDeberiaDevolverNullSiNoEsUrl() {
        // Ruta local heredada de una version anterior (ya no deberia poder crearse una nueva
        // asi, pero si existiera una en la base de datos, no debe romper la generacion del
        // documento: simplemente no se muestra ese logo).
        ConfiguracionService.ImagenDocumento resultado = configuracionService.resolverImagenParaDocumento(
                "/config/logo/antiguo.png", TipoArchivoInstitucional.LOGO, 90, 90);
        assertNull(resultado);
    }
}
