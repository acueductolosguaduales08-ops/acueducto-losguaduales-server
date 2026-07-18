package com.acueducto.backend.service;

import com.acueducto.backend.dto.request.CategoriaRequest;
import com.acueducto.backend.dto.request.EtiquetaRequest;
import com.acueducto.backend.dto.request.PublicacionRequest;
import com.acueducto.backend.dto.request.VideoRequest;
import com.acueducto.backend.dto.response.PublicacionResponse;
import com.acueducto.backend.dto.response.ReaccionResponse;
import com.acueducto.backend.entity.*;
import com.acueducto.backend.entity.enums.EstadoPublicacion;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.exception.ReglaNegocioException;
import com.acueducto.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Modulo del Portal Publico: galeria, publicaciones destacadas, categorias, etiquetas, videos y reacciones (Modulo 11). */
@Service
@RequiredArgsConstructor
public class PublicacionService {

    private static final int LONGITUD_MAXIMA_EMOJI = 16;

    private final PublicacionRepository publicacionRepository;
    private final CategoriaRepository categoriaRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final VideoRepository videoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReaccionPublicacionRepository reaccionPublicacionRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public PublicacionResponse crear(PublicacionRequest request, String autorUsername) {
        Usuario autor = usuarioRepository.findByUsernameIgnoreCase(autorUsername)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Categoria categoria = null;
        if (request.categoriaId() != null) {
            categoria = categoriaRepository.findById(request.categoriaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Categoria no encontrada"));
        }

        Set<Etiqueta> etiquetas = new HashSet<>();
        if (request.etiquetasIds() != null) {
            etiquetas.addAll(etiquetaRepository.findAllById(request.etiquetasIds()));
        }

        Publicacion publicacion = Publicacion.builder()
                .titulo(request.titulo())
                .descripcionCorta(request.descripcionCorta())
                .contenidoCompleto(request.contenidoCompleto())
                .imagenUrl(request.imagenUrl())
                .posicionImagen(request.posicionImagen())
                .categoria(categoria)
                .etiquetas(etiquetas)
                .autor(autor)
                .estado(EstadoPublicacion.BORRADOR)
                .destacada(request.destacada())
                .build();

        publicacion = publicacionRepository.save(publicacion);
        auditoriaService.registrar("CREAR_PUBLICACION", "PORTAL_PUBLICO", publicacion.getTitulo(), null);
        return PublicacionResponse.fromEntity(publicacion);
    }

    @Transactional
    public PublicacionResponse publicar(Long id) {
        Publicacion publicacion = obtenerEntidad(id);
        publicacion.setEstado(EstadoPublicacion.PUBLICADA);
        publicacion = publicacionRepository.save(publicacion);
        auditoriaService.registrar("PUBLICAR_CONTENIDO", "PORTAL_PUBLICO", publicacion.getTitulo(), null);
        return PublicacionResponse.fromEntity(publicacion);
    }

    @Transactional
    public PublicacionResponse ocultar(Long id) {
        Publicacion publicacion = obtenerEntidad(id);
        publicacion.setEstado(EstadoPublicacion.OCULTA);
        publicacion = publicacionRepository.save(publicacion);
        return PublicacionResponse.fromEntity(publicacion);
    }

    @Transactional
    public PublicacionResponse destacar(Long id, boolean destacada) {
        Publicacion publicacion = obtenerEntidad(id);
        publicacion.setDestacada(destacada);
        publicacion = publicacionRepository.save(publicacion);
        return PublicacionResponse.fromEntity(publicacion);
    }

    @Transactional
    public PublicacionResponse actualizar(Long id, PublicacionRequest request) {
        Publicacion publicacion = obtenerEntidad(id);

        Categoria categoria = null;
        if (request.categoriaId() != null) {
            categoria = categoriaRepository.findById(request.categoriaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Categoria no encontrada"));
        }

        Set<Etiqueta> etiquetas = new HashSet<>();
        if (request.etiquetasIds() != null) {
            etiquetas.addAll(etiquetaRepository.findAllById(request.etiquetasIds()));
        }

        publicacion.setTitulo(request.titulo());
        publicacion.setDescripcionCorta(request.descripcionCorta());
        publicacion.setContenidoCompleto(request.contenidoCompleto());
        publicacion.setImagenUrl(request.imagenUrl());
        publicacion.setPosicionImagen(request.posicionImagen());
        publicacion.setCategoria(categoria);
        publicacion.setEtiquetas(etiquetas);
        publicacion.setDestacada(request.destacada());

        publicacion = publicacionRepository.save(publicacion);
        auditoriaService.registrar("EDITAR_PUBLICACION", "PORTAL_PUBLICO", publicacion.getTitulo(), null);
        return PublicacionResponse.fromEntity(publicacion);
    }

    /** Solo el Administrador puede eliminar definitivamente una publicacion (11.10). */
    @Transactional
    public void eliminar(Long id) {
        Publicacion publicacion = obtenerEntidad(id);
        publicacionRepository.delete(publicacion);
        auditoriaService.registrar("ELIMINAR_PUBLICACION", "PORTAL_PUBLICO", publicacion.getTitulo(), null);
    }

    public Publicacion obtenerEntidad(Long id) {
        return publicacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Publicacion no encontrada con id " + id));
    }

    public PublicacionResponse obtener(Long id) {
        return PublicacionResponse.fromEntity(obtenerEntidad(id));
    }

    public Page<PublicacionResponse> listarPublicadas(Pageable pageable) {
        return publicacionRepository.findByEstado(EstadoPublicacion.PUBLICADA, pageable).map(PublicacionResponse::fromEntity);
    }

    /** Historial completo (cualquier estado: borrador, publicada u oculta), para el panel de edicion. */
    public Page<PublicacionResponse> listarTodas(Pageable pageable) {
        return publicacionRepository.findAll(pageable).map(PublicacionResponse::fromEntity);
    }

    public List<PublicacionResponse> listarDestacadas() {
        return publicacionRepository.findByEstadoAndDestacadaTrue(EstadoPublicacion.PUBLICADA).stream()
                .map(PublicacionResponse::fromEntity).toList();
    }

    // ---- Categorias ----
    @Transactional
    public Categoria crearCategoria(CategoriaRequest request) {
        return categoriaRepository.save(Categoria.builder().nombre(request.nombre()).build());
    }

    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }

    // ---- Etiquetas ----
    @Transactional
    public Etiqueta crearEtiqueta(EtiquetaRequest request) {
        return etiquetaRepository.save(Etiqueta.builder().nombre(request.nombre()).color(request.color()).build());
    }

    public List<Etiqueta> listarEtiquetas() {
        return etiquetaRepository.findAll();
    }

    // ---- Videos ----
    @Transactional
    public Video crearVideo(VideoRequest request) {
        Video video = Video.builder()
                .titulo(request.titulo()).descripcion(request.descripcion()).urlVideo(request.urlVideo()).visible(true)
                .build();
        return videoRepository.save(video);
    }

    public List<Video> listarVideosVisibles() {
        return videoRepository.findByVisibleTrue();
    }

    @Transactional
    public void ocultarVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Video no encontrado"));
        video.setVisible(false);
        videoRepository.save(video);
    }

    // ---- Reacciones (publico, sin autenticacion) ----

    /**
     * Suma una reaccion (emoji) a una publicacion/imagen. No se guarda quien reacciono,
     * solo se incrementa el contador total de ese emoji para esa publicacion (11.x).
     * Evitar que un mismo dispositivo reaccione varias veces es responsabilidad del
     * frontend (se guarda localmente que ya reacciono).
     */
    @Transactional
    public ReaccionResponse reaccionar(Long publicacionId, String emoji) {
        String emojiNormalizado = normalizarEmoji(emoji);
        if (!publicacionRepository.existsById(publicacionId)) {
            throw new RecursoNoEncontradoException("Publicacion no encontrada con id " + publicacionId);
        }

        int filasActualizadas = reaccionPublicacionRepository.incrementar(publicacionId, emojiNormalizado);
        if (filasActualizadas == 0) {
            try {
                Publicacion publicacion = obtenerEntidad(publicacionId);
                ReaccionPublicacion nueva = ReaccionPublicacion.builder()
                        .publicacion(publicacion).emoji(emojiNormalizado).contador(1L).build();
                reaccionPublicacionRepository.save(nueva);
            } catch (DataIntegrityViolationException concurrencia) {
                // Otra peticion concurrente creo la fila primero: se reintenta el incremento atomico.
                reaccionPublicacionRepository.incrementar(publicacionId, emojiNormalizado);
            }
        }

        return reaccionPublicacionRepository.findByPublicacionIdAndEmoji(publicacionId, emojiNormalizado)
                .map(ReaccionResponse::fromEntity)
                .orElse(ReaccionResponse.builder().emoji(emojiNormalizado).contador(1L).build());
    }

    /** Resta una reaccion (usado cuando el usuario la retira desde su dispositivo). Nunca baja de cero. */
    @Transactional
    public void quitarReaccion(Long publicacionId, String emoji) {
        reaccionPublicacionRepository.decrementar(publicacionId, normalizarEmoji(emoji));
    }

    public List<ReaccionResponse> listarReacciones(Long publicacionId) {
        return reaccionPublicacionRepository.findByPublicacionIdOrderByContadorDesc(publicacionId).stream()
                .map(ReaccionResponse::fromEntity).toList();
    }

    private String normalizarEmoji(String emoji) {
        if (emoji == null || emoji.isBlank()) {
            throw new ReglaNegocioException("Debe indicar el emoji con el que desea reaccionar.");
        }
        String limpio = emoji.trim();
        if (limpio.length() > LONGITUD_MAXIMA_EMOJI) {
            throw new ReglaNegocioException("El emoji indicado no es valido.");
        }
        return limpio;
    }
}
