package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.CrearConversacionRequest;
import com.acueducto.backend.dto.request.CrearMensajeRequest;
import com.acueducto.backend.dto.request.EditarMensajeRequest;
import com.acueducto.backend.dto.response.ConversacionResponse;
import com.acueducto.backend.dto.response.MensajeResponse;
import com.acueducto.backend.dto.response.ParticipanteResponse;
import com.acueducto.backend.dto.response.SolicitudEliminacionResponse;
import com.acueducto.backend.security.UserPrincipal;
import com.acueducto.backend.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat interno de la plataforma (Asociado <-> Admin/Tesorero y entre administrativos).
 * Solo texto y emojis. La eliminacion de mensajes exige solicitud + confirmacion del
 * otro participante; la eliminacion completa de conversaciones es exclusiva de
 * ADMINISTRADOR/TESORERO.
 */
@Tag(name = "18. Chat", description = "Mensajeria interna de texto/emojis entre Asociado, Tesorero y Administrador. La eliminacion de mensajes requiere solicitud y confirmacion mutua; los mensajes se conservan en PostgreSQL solo 8 dias")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','TESORERO','ASOCIADO')")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Contactos disponibles para chat",
            description = "Devuelve la lista de usuarios con los que el autenticado puede iniciar una conversacion. Admin/Tesorero ven asociados; Asociado ve administradores y tesoreros.")
    @GetMapping("/contactos")
    public ResponseEntity<List<ParticipanteResponse>> obtenerContactos(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.obtenerContactos(principal.getId()));
    }

    @Operation(summary = "Crear u obtener conversacion",
            description = "Crea una conversacion con el usuario destinatario, o devuelve la existente si ya hay una entre ambos. Nunca se crean conversaciones duplicadas.")
    @PostMapping("/conversaciones")
    public ResponseEntity<ConversacionResponse> crearConversacion(
            @Valid @RequestBody CrearConversacionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.crearConversacion(principal.getId(), request.destinatarioId()));
    }

    @Operation(summary = "Listar conversaciones del usuario autenticado",
            description = "Devuelve las conversaciones del usuario con participante, ultimo mensaje, no leidos e indicadores de solicitudes de eliminacion pendientes.")
    @GetMapping("/conversaciones")
    public ResponseEntity<List<ConversacionResponse>> listarConversaciones(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.listarConversaciones(principal.getId()));
    }

    @Operation(summary = "Obtener mensajes de una conversacion",
            description = "Devuelve los mensajes aun disponibles en PostgreSQL. Opcionalmente con ?desde={id} o ?desdeFecha={fechaISO} para sincronizar solo lo nuevo.")
    @GetMapping("/conversaciones/{id}/mensajes")
    public ResponseEntity<List<MensajeResponse>> listarMensajes(
            @PathVariable Long id,
            @RequestParam(required = false) Long desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desdeFecha,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.listarMensajes(principal.getId(), id, desde, desdeFecha));
    }

    @Operation(summary = "Enviar mensaje",
            description = "Solo texto y emojis. El remitente se obtiene del JWT; nunca se confia en un usuarioId del frontend.")
    @PostMapping("/conversaciones/{id}/mensajes")
    public ResponseEntity<MensajeResponse> enviarMensaje(
            @PathVariable Long id,
            @Valid @RequestBody CrearMensajeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.enviarMensaje(principal.getId(), id, request.contenido()));
    }

    @Operation(summary = "Editar mensaje propio",
            description = "Solo el autor puede editar. Se conservan autor y fecha original; se marca como editado.")
    @PatchMapping("/mensajes/{id}")
    public ResponseEntity<MensajeResponse> editarMensaje(
            @PathVariable Long id,
            @Valid @RequestBody EditarMensajeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.editarMensaje(principal.getId(), id, request.contenido()));
    }

    @Operation(summary = "Solicitar eliminacion de un mensaje propio",
            description = "Crea una solicitud PENDIENTE. El mensaje NO se elimina todavia: requiere que el otro participante la acepte.")
    @PostMapping("/mensajes/{mensajeId}/solicitud-eliminacion")
    public ResponseEntity<SolicitudEliminacionResponse> solicitarEliminacion(
            @PathVariable Long mensajeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.solicitarEliminacion(principal.getId(), mensajeId));
    }

    @Operation(summary = "Listar solicitudes de eliminacion pendientes",
            description = "Devuelve las solicitudes pendientes del usuario autenticado: las que debe confirmar (CONFIRMADOR) y las que envio (SOLICITANTE).")
    @GetMapping("/solicitudes-eliminacion")
    public ResponseEntity<List<SolicitudEliminacionResponse>> listarSolicitudes(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.listarSolicitudes(principal.getId()));
    }

    @Operation(summary = "Aceptar solicitud de eliminacion",
            description = "Solo el confirmador puede aceptar. Elimina definitivamente el mensaje en una transaccion; el frontend debe eliminarlo tambien de IndexedDB.")
    @PatchMapping("/solicitudes-eliminacion/{id}/aceptar")
    public ResponseEntity<SolicitudEliminacionResponse> aceptarSolicitud(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.aceptarSolicitud(principal.getId(), id));
    }

    @Operation(summary = "Rechazar solicitud de eliminacion",
            description = "Solo el confirmador puede rechazar. El mensaje se conserva y vuelve a mostrarse normalmente.")
    @PatchMapping("/solicitudes-eliminacion/{id}/rechazar")
    public ResponseEntity<SolicitudEliminacionResponse> rechazarSolicitud(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.rechazarSolicitud(principal.getId(), id));
    }

    @Operation(summary = "Cancelar solicitud de eliminacion",
            description = "Solo el solicitante puede cancelar su propia solicitud pendiente.")
    @PatchMapping("/solicitudes-eliminacion/{id}/cancelar")
    public ResponseEntity<SolicitudEliminacionResponse> cancelarSolicitud(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatService.cancelarSolicitud(principal.getId(), id));
    }

    @Operation(summary = "Marcar mensajes como leidos",
            description = "Marca como leidos los mensajes recibidos por el usuario autenticado en la conversacion.")
    @PatchMapping("/conversaciones/{id}/leidos")
    public ResponseEntity<Void> marcarLeidos(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        chatService.marcarLeidos(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar conversacion completa",
            description = "Definitivo. Exclusivo de ADMINISTRADOR/TESORERO. Elimina conversacion, mensajes y solicitudes relacionadas en una transaccion. El asociado no puede usar este endpoint.")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','TESORERO')")
    @DeleteMapping("/conversaciones/{id}")
    public ResponseEntity<Void> eliminarConversacion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        chatService.eliminarConversacion(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}