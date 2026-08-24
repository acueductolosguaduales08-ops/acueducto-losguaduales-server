package com.acueducto.backend.service;

import com.acueducto.backend.dto.response.VerificacionBorradoResponse;
import com.acueducto.backend.entity.*;
import com.acueducto.backend.entity.enums.EstadoFactura;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.exception.ReglaNegocioException;
import com.acueducto.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Modulo "Gestion de datos importantes" (punto 5 del pedido): eliminacion DEFINITIVA (borrado
 * fisico, irreversible) de todos los tipos de datos del sistema. Exclusivo del Administrador.
 *
 * Diseno: borrado en CASCADA TOTAL. Nada queda bloqueado por tener datos relacionados: si el
 * registro que se quiere borrar tiene historial, ese historial tambien se borra (en el orden
 * correcto para no romper llaves foraneas). La unica excepcion es que un Administrador no puede
 * borrar su propia cuenta de sesion (se dejaria sin acceso al sistema), y aun asi se puede
 * usando otra cuenta.
 *
 * Cada operacion:
 * 1. Reconfirma la contrasena del Administrador (verificarPassword), aunque ya tenga sesion.
 * 2. Antes de borrar, el frontend consulta GET /verificar para informar al usuario que se va a
 *    borrar (y en cascada). El borrado real ejecuta exactamente lo informado.
 * 3. Ejecuta la cascada en una sola transaccion y registra el borrado en auditoria.
 */
@Service
@RequiredArgsConstructor
public class GestionDatosImportantesService {

    private final AuthService authService;
    private final AuditoriaService auditoriaService;

    private final EncuestaRepository encuestaRepository;
    private final RespuestaEncuestaRepository respuestaEncuestaRepository;
    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;
    private final ReciboRepository reciboRepository;
    private final MultaRepository multaRepository;
    private final AsociadoRepository asociadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedidorRepository medidorRepository;
    private final LecturaRepository lecturaRepository;
    private final MesContableRepository mesContableRepository;
    private final AnioContableRepository anioContableRepository;
    private final MovimientoTesoreriaRepository movimientoTesoreriaRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionLecturaRepository notificacionLecturaRepository;
    private final PublicacionRepository publicacionRepository;
    private final ReaccionPublicacionRepository reaccionPublicacionRepository;
    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final SolicitudEliminacionRepository solicitudEliminacionRepository;
    private final SupabaseStorageService supabaseStorageService;

    // =============================== FORMULARIOS ===============================

    /** Borra el formulario y, en cascada, todas sus preguntas y las respuestas ya recibidas. */
    @Transactional
    public void eliminarFormulario(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Encuesta encuesta = encuestaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Formulario no encontrado con id " + id));
        eliminarEncuestaCascada(encuesta);
        auditoriaService.registrar("ELIMINAR_FORMULARIO_DEFINITIVO", "DATOS_IMPORTANTES", encuesta.getCodigo(), "id=" + id);
    }

    private void eliminarEncuestaCascada(Encuesta encuesta) {
        respuestaEncuestaRepository.deleteAll(respuestaEncuestaRepository.findByEncuestaId(encuesta.getId()));
        encuestaRepository.delete(encuesta); // cascada: PreguntaEncuesta (orphanRemoval)
    }

    // =============================== FACTURAS ===============================

    /**
     * Borra la factura y TODO su historial en cascada: pagos (con sus recibos y movimientos de
     * tesoreria), conceptos adicionales, multas que apuntaban a ella (quedan sueltas) y la
     * lectura que la genero (vuelve a quedar disponible para facturar).
     */
    @Transactional
    public void eliminarFactura(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada con id " + id));
        String numero = factura.getNumeroFactura();
        eliminarFacturaCascada(factura);
        auditoriaService.registrar("ELIMINAR_FACTURA_DEFINITIVA", "DATOS_IMPORTANTES", numero, "id=" + id);
    }

    private void eliminarFacturaCascada(Factura factura) {
        for (Pago pago : pagoRepository.findByFacturaId(factura.getId())) {
            eliminarPagoCompleto(pago);
        }
        // Movimientos de tesoreria que apunten a la factura directamente (ingresos extraordinarios)
        movimientoTesoreriaRepository.findByFacturaId(factura.getId()).forEach(movimientoTesoreriaRepository::delete);
        // Multas que apuntaban a esta factura: se desvinculan y quedan sueltas
        multaRepository.findByFacturaId(factura.getId()).forEach(m -> {
            m.setFactura(null);
            multaRepository.save(m);
        });
        // La lectura que la genero vuelve a quedar disponible
        if (factura.getLectura() != null) {
            Lectura lectura = factura.getLectura();
            lectura.setFacturaGenerada(false);
            lecturaRepository.save(lectura);
        }
        facturaRepository.delete(factura); // cascada: ConceptoFactura (orphanRemoval)
    }

    // =============================== RECIBOS ===============================

    /**
     * Borra el recibo y sus movimientos de tesoreria. El pago y la factura asociados NO se tocan:
     * es una eliminacion de constancia en papel, no una reversa contable.
     */
    @Transactional
    public void eliminarRecibo(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Recibo recibo = reciboRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recibo no encontrado con id " + id));
        String numero = recibo.getNumeroRecibo();
        movimientoTesoreriaRepository.findByReciboId(id).forEach(movimientoTesoreriaRepository::delete);
        reciboRepository.delete(recibo);
        auditoriaService.registrar("ELIMINAR_RECIBO_DEFINITIVO", "DATOS_IMPORTANTES", numero, "id=" + id);
    }

    // =============================== ASOCIADOS ===============================

    /**
     * Borra el asociado y TODO su historial en cascada: facturas (con pagos, recibos, movimientos
     * y conceptos), multas, lecturas, movimientos de tesoreria que lo referencien y, si tiene, su
     * cuenta de usuario (con sus dependencias). El medidor NO se borra: solo se desvincula y queda
     * disponible.
     */
    @Transactional
    public void eliminarAsociado(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Asociado asociado = asociadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asociado no encontrado con id " + id));

        // Cuenta de usuario vinculada (si existe) -> cascada de sus dependencias
        usuarioRepository.findByAsociadoId(id).ifPresent(this::eliminarCuentaCascada);
        // Pagos de multas independientes (borrar antes que las multas para no romper FK)
        for (Multa multa : multaRepository.findByAsociadoId(id)) {
            for (Pago pagoMulta : pagoRepository.findByMultaId(multa.getId())) {
                eliminarPagoCompleto(pagoMulta);
            }
        }
        // Multas
        multaRepository.findByAsociadoId(id).forEach(multaRepository::delete);
        // Facturas (cascada total)
        for (Factura factura : facturaRepository.findByAsociadoId(id, Pageable.unpaged()).getContent()) {
            eliminarFacturaCascada(factura);
        }
        // Lecturas (sus facturas ya fueron borradas/liberadas)
        for (Lectura lectura : lecturaRepository.findByAsociadoIdOrderByFechaLecturaDesc(id)) {
            facturaRepository.findByLecturaId(lectura.getId()).ifPresent(this::eliminarFacturaCascada);
            lecturaRepository.delete(lectura);
        }
        // Movimientos de tesoreria que referencian al asociado
        movimientoTesoreriaRepository.findByAsociadoId(id).forEach(movimientoTesoreriaRepository::delete);
        // El medidor NO se borra: se desvincula y queda disponible
        if (asociado.getMedidor() != null) {
            Medidor medidor = asociado.getMedidor();
            medidor.setAsociado(null);
            medidorRepository.save(medidor);
        }

        String codigo = asociado.getCodigoInterno();
        asociadoRepository.delete(asociado);
        auditoriaService.registrar("ELIMINAR_ASOCIADO_DEFINITIVO", "DATOS_IMPORTANTES", codigo, "id=" + id);
    }

    // =============================== CUENTAS ===============================

    /**
     * Borra una cuenta de usuario en cascada: formularios que creo, notificaciones que envio o
     * recibio, lecturas de notificaciones, respuestas de formularios (se desvinculan), pagos que
     * registro como tesorero (con sus recibos y movimientos, recalculando la factura) y
     * movimientos que registro. Unica excepcion: no se puede borrar la propia cuenta en uso.
     */
    @Transactional
    public void eliminarCuenta(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada con id " + id));
        if (usuario.getUsername().equalsIgnoreCase(usernameAdmin)) {
            throw new ReglaNegocioException("No puede eliminar su propia cuenta mientras la esta usando. Use otra cuenta de Administrador.");
        }
        String username = usuario.getUsername();
        eliminarCuentaCascada(usuario);
        auditoriaService.registrar("ELIMINAR_CUENTA_DEFINITIVA", "DATOS_IMPORTANTES", username, "id=" + id);
    }

    private void eliminarCuentaCascada(Usuario usuario) {
        Long uid = usuario.getId();

        // Publicaciones que creo (borra reacciones en cascada; etiquetasManyToMany se desvinculan solas)
        for (Publicacion pub : publicacionRepository.findByAutorId(uid)) {
            supabaseStorageService.eliminarPorUrl(pub.getImagenUrl());
            reaccionPublicacionRepository.deleteAll(reaccionPublicacionRepository.findByPublicacionIdOrderByContadorDesc(pub.getId()));
            publicacionRepository.delete(pub);
        }

        // Formularios que creo
        encuestaRepository.findByAutorId(uid).forEach(this::eliminarEncuestaCascada);

        // Notificaciones que autorizo o que le fueron dirigidas (sin duplicar)
        List<Notificacion> notificaciones = new java.util.ArrayList<>(notificacionRepository.findByAutorId(uid));
        for (Notificacion n : notificacionRepository.findByDestinatarioId(uid)) {
            if (notificaciones.stream().noneMatch(x -> x.getId().equals(n.getId()))) {
                notificaciones.add(n);
            }
        }
        for (Notificacion n : notificaciones) {
            eliminarNotificacionCascada(n);
        }

        // Registros de lectura de notificaciones
        notificacionLecturaRepository.findByUsuarioId(uid).forEach(notificacionLecturaRepository::delete);

        // Respuestas de formularios: se desvinculan del usuario (quedan como publicas/anonimas)
        respuestaEncuestaRepository.findByUsuarioId(uid).forEach(r -> {
            r.setUsuario(null);
            respuestaEncuestaRepository.save(r);
        });

        // Pagos que registro como tesorero (cascada recibo + movimiento + recalculando la factura)
        for (Pago pago : pagoRepository.findByTesoreroId(uid)) {
            Factura factura = pago.getFactura();
            eliminarPagoCompleto(pago);
            if (factura != null) recalcularFactura(factura);
        }

        // Movimientos de tesoreria que registro
        movimientoTesoreriaRepository.findByUsuarioId(uid).forEach(movimientoTesoreriaRepository::delete);

        // Conversaciones de chat donde participa (borra solicitudes, mensajes y la conversacion)
        List<Conversacion> conversaciones = conversacionRepository.findByUsuario1IdOrUsuario2Id(uid, uid);
        for (Conversacion conv : conversaciones) {
            solicitudEliminacionRepository.deleteByMensaje_ConversacionId(conv.getId());
            mensajeRepository.deleteByConversacionId(conv.getId());
            conversacionRepository.delete(conv);
        }

        // Solicitudes de eliminacion donde fue solicitante o confirmador
        // (las de conversaciones ya se limpiaron arriba; esto cubre las restantes)
        solicitudEliminacionRepository.deleteByUsuarioId(uid);

        usuarioRepository.delete(usuario);
    }

    // =============================== PERIODOS CONTABLES ===============================

    /**
     * Borra un mes contable y todo lo que contiene en cascada: facturas (con su historial),
     * lecturas y movimientos de tesoreria del mes.
     */
    @Transactional
    public void eliminarPeriodoContable(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        MesContable mes = mesContableRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Periodo contable no encontrado con id " + id));
        String nombre = mes.getAnioContable().getAnio() + "-" + mes.getNumeroMes();
        eliminarMesCascada(mes);
        auditoriaService.registrar("ELIMINAR_PERIODO_CONTABLE_DEFINITIVO", "DATOS_IMPORTANTES", nombre, "id=" + id);
    }

    /** Borra un anio contable y, en cascada, todos sus meses con todo lo que contengan. */
    @Transactional
    public void eliminarAnioContable(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        AnioContable anio = anioContableRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Anio contable no encontrado con id " + id));
        for (MesContable mes : mesContableRepository.findByAnioContableIdOrderByNumeroMes(anio.getId())) {
            eliminarMesCascada(mes);
        }
        anioContableRepository.delete(anio);
        auditoriaService.registrar("ELIMINAR_ANIO_CONTABLE_DEFINITIVO", "DATOS_IMPORTANTES", String.valueOf(anio.getAnio()), "id=" + id);
    }

    private void eliminarMesCascada(MesContable mes) {
        for (Factura factura : facturaRepository.findByMesContableId(mes.getId())) {
            eliminarFacturaCascada(factura);
        }
        for (Lectura lectura : lecturaRepository.findByMesContableId(mes.getId())) {
            facturaRepository.findByLecturaId(lectura.getId()).ifPresent(this::eliminarFacturaCascada);
            lecturaRepository.delete(lectura);
        }
        movimientoTesoreriaRepository.findByMesContableId(mes.getId()).forEach(movimientoTesoreriaRepository::delete);
        mesContableRepository.delete(mes);
    }

    // =============================== MULTAS ===============================

    /**
     * Borra la multa. Si ya estaba incluida en una factura, descuenta su valor del total de esa
     * factura para no dejar un cobro fantasma.
     */
    @Transactional
    public void eliminarMulta(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Multa multa = multaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Multa no encontrada con id " + id));
        if (multa.getFactura() != null) {
            Factura factura = multa.getFactura();
            factura.setTotalMultas(factura.getTotalMultas().subtract(multa.getValor()));
            factura.setTotal(factura.getTotal().subtract(multa.getValor()));
            facturaRepository.save(factura);
        }
        String motivo = multa.getMotivo();
        for (Pago pagoMulta : pagoRepository.findByMultaId(id)) {
            eliminarPagoCompleto(pagoMulta);
        }
        multaRepository.delete(multa);
        auditoriaService.registrar("ELIMINAR_MULTA_DEFINITIVA", "DATOS_IMPORTANTES", motivo, "id=" + id);
    }

    // =============================== MEDIDORES ===============================

    /**
     * Borra el medidor definitivamente. Si esta vinculado a un asociado, lo desvincula
     * automaticamente (el asociado NO se borra). Si tiene lecturas registradas, se borran en
     * cascada (junto con las facturas que hayan generado).
     */
    @Transactional
    public void eliminarMedidor(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Medidor medidor = medidorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Medidor no encontrado con id " + id));
        for (Lectura lectura : lecturaRepository.findByMedidorId(id)) {
            facturaRepository.findByLecturaId(lectura.getId()).ifPresent(this::eliminarFacturaCascada);
            lecturaRepository.delete(lectura);
        }
        if (medidor.getAsociado() != null) {
            medidor.setAsociado(null);
            medidorRepository.save(medidor);
        }
        String numero = medidor.getNumero();
        medidorRepository.delete(medidor);
        auditoriaService.registrar("ELIMINAR_MEDIDOR_DEFINITIVO", "DATOS_IMPORTANTES", numero, "id=" + id);
    }

    // =============================== LECTURAS ===============================

    /** Borra la lectura. Si ya genero una factura, la factura se borra en cascada. */
    @Transactional
    public void eliminarLectura(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Lectura lectura = lecturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lectura no encontrada con id " + id));
        facturaRepository.findByLecturaId(id).ifPresent(this::eliminarFacturaCascada);
        lecturaRepository.delete(lectura);
        auditoriaService.registrar("ELIMINAR_LECTURA_DEFINITIVA", "DATOS_IMPORTANTES",
                "Medidor " + lectura.getMedidor().getNumero(), "id=" + id);
    }

    // =============================== PAGOS ===============================

    /**
     * Borra el pago en cascada: su recibo, los movimientos de tesoreria del recibo, y recalcula
     * el totalPagado y estado de la factura (reversa contable real).
     */
    @Transactional
    public void eliminarPago(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado con id " + id));
        Factura factura = pago.getFactura();
        eliminarPagoCompleto(pago);
        if (factura != null) recalcularFactura(factura);
        auditoriaService.registrar("ELIMINAR_PAGO_DEFINITIVO", "DATOS_IMPORTANTES", factura != null ? factura.getNumeroFactura() : "Multa independiente", "id=" + id);
    }

    private void eliminarPagoCompleto(Pago pago) {
        reciboRepository.findByPagoId(pago.getId()).ifPresent(recibo -> {
            movimientoTesoreriaRepository.findByReciboId(recibo.getId()).forEach(movimientoTesoreriaRepository::delete);
            reciboRepository.delete(recibo);
        });
        pagoRepository.delete(pago);
    }

    private void recalcularFactura(Factura factura) {
        BigDecimal pagado = pagoRepository.findByFacturaId(factura.getId()).stream()
                .map(Pago::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        factura.setTotalPagado(pagado);
        BigDecimal saldo = factura.getTotal().subtract(pagado);
        if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
            factura.setEstado(EstadoFactura.PAGADA);
        } else if (pagado.compareTo(BigDecimal.ZERO) > 0) {
            factura.setEstado(EstadoFactura.PAGADA_PARCIAL);
        } else {
            factura.setEstado(EstadoFactura.PENDIENTE);
        }
        facturaRepository.save(factura);
    }

    // =============================== MOVIMIENTOS DE TESORERIA ===============================

    /**
     * Borra un movimiento de tesoreria. Si es una entrada generada por un pago (tiene recibo),
     * el pago se borra en cascada (recibo + movimiento + recalculo de la factura); si es un
     * ingreso/gasto suelto, solo se borra el movimiento.
     */
    @Transactional
    public void eliminarMovimientoTesoreria(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        MovimientoTesoreria movimiento = movimientoTesoreriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Movimiento no encontrado con id " + id));
        if (movimiento.getRecibo() != null) {
            Pago pago = movimiento.getRecibo().getPago();
            Factura factura = pago.getFactura();
            eliminarPagoCompleto(pago);
            if (factura != null) recalcularFactura(factura);
        } else {
            movimientoTesoreriaRepository.delete(movimiento);
        }
        auditoriaService.registrar("ELIMINAR_MOVIMIENTO_DEFINITIVO", "DATOS_IMPORTANTES",
                movimiento.getId().toString(), "id=" + id);
    }

    // =============================== NOTIFICACIONES ===============================

    /** Borra la notificacion junto con todos sus registros de lectura. */
    @Transactional
    public void eliminarNotificacion(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificacion no encontrada con id " + id));
        String titulo = notificacion.getTitulo();
        eliminarNotificacionCascada(notificacion);
        auditoriaService.registrar("ELIMINAR_NOTIFICACION_DEFINITIVA", "DATOS_IMPORTANTES", titulo, "id=" + id);
    }

    private void eliminarNotificacionCascada(Notificacion notificacion) {
        notificacionLecturaRepository.findByNotificacionId(notificacion.getId()).forEach(notificacionLecturaRepository::delete);
        notificacionRepository.delete(notificacion);
    }

    // =============================== VERIFICACION (diagnostico previo) ===============================

    /**
     * Diagnostico previo a la eliminacion: informa si el registro se puede borrar y, en cascada,
     * exactamente que se va a eliminar. El frontend debe mostrar esto antes de pedir la contrasena.
     */
    public VerificacionBorradoResponse verificar(String tipo, Long id, String usernameActual) {
        return switch (tipo.toUpperCase()) {
            case "FORMULARIO" -> verificarFormulario(id);
            case "FACTURA" -> verificarFactura(id);
            case "RECIBO" -> verificarRecibo(id);
            case "ASOCIADO" -> verificarAsociado(id);
            case "CUENTA" -> verificarCuenta(id, usernameActual);
            case "PERIODO_CONTABLE" -> verificarPeriodo(id);
            case "ANIO_CONTABLE" -> verificarAnio(id);
            case "MULTA" -> verificarMulta(id);
            case "MEDIDOR" -> verificarMedidor(id);
            case "LECTURA" -> verificarLectura(id);
            case "PAGO" -> verificarPago(id);
            case "MOVIMIENTO" -> verificarMovimiento(id);
            case "NOTIFICACION" -> verificarNotificacion(id);
            default -> throw new RecursoNoEncontradoException("Tipo de dato no soportado: " + tipo);
        };
    }

    private VerificacionBorradoResponse verificarFormulario(Long id) {
        Encuesta e = encuestaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Formulario no encontrado con id " + id));
        VerificacionBorradoResponse r = VerificacionBorradoResponse.borrable("FORMULARIO", id, e.getCodigo(),
                "Se borrara el formulario con sus preguntas y las respuestas ya recibidas.");
        r.getCascada().put("preguntas", (long) e.getPreguntas().size());
        r.getCascada().put("respuestas", respuestaEncuestaRepository.countByEncuestaId(id));
        return r;
    }

    private VerificacionBorradoResponse verificarFactura(Long id) {
        Factura f = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada con id " + id));
        VerificacionBorradoResponse r = VerificacionBorradoResponse.borrable("FACTURA", id, f.getNumeroFactura(),
                "Se borrara la factura con todo su historial de pagos/recibos y los conceptos asociados.");
        List<Pago> pagos = pagoRepository.findByFacturaId(id);
        r.getCascada().put("conceptos", (long) f.getConceptos().size());
        r.getCascada().put("pagos", (long) pagos.size());
        long recibos = 0;
        long movimientos = movimientoTesoreriaRepository.findByFacturaId(id).stream()
                .filter(m -> m.getRecibo() == null).count();
        for (Pago pago : pagos) {
            if (reciboRepository.findByPagoId(pago.getId()).isPresent()) {
                recibos++;
                Recibo recibo = reciboRepository.findByPagoId(pago.getId()).get();
                movimientos += movimientoTesoreriaRepository.findByReciboId(recibo.getId()).size();
            }
        }
        r.getCascada().put("recibos", recibos);
        r.getCascada().put("movimientosTesoreria", movimientos);
        r.getCascada().put("multasDesvinculadas", (long) multaRepository.findByFacturaId(id).size());
        r.getCascada().put("lecturaLiberada", f.getLectura() != null ? 1L : 0L);
        return r;
    }

    private VerificacionBorradoResponse verificarRecibo(Long id) {
        Recibo recibo = reciboRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recibo no encontrado con id " + id));
        VerificacionBorradoResponse r = VerificacionBorradoResponse.borrable("RECIBO", id, recibo.getNumeroRecibo(),
                "Se borrara el recibo. El pago y la factura asociados NO se tocan.");
        r.getCascada().put("movimientosTesoreria", (long) movimientoTesoreriaRepository.findByReciboId(id).size());
        return r;
    }

    private VerificacionBorradoResponse verificarAsociado(Long id) {
        Asociado a = asociadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asociado no encontrado con id " + id));
        VerificacionBorradoResponse r = VerificacionBorradoResponse.borrable("ASOCIADO", id, a.getCodigoInterno(),
                "Se borrara el asociado y TODO su historial en cascada. El medidor solo se desvincula, NO se borra.");
        long facturas = facturaRepository.findByAsociadoId(id, Pageable.unpaged()).getContent().size();
        long pagos = pagoRepository.findByAsociadoId(id).size();
        long recibos = 0;
        for (Pago pago : pagoRepository.findByAsociadoId(id)) {
            if (reciboRepository.findByPagoId(pago.getId()).isPresent()) recibos++;
        }
        r.getCascada().put("facturas", facturas);
        r.getCascada().put("pagos", pagos);
        r.getCascada().put("recibos", recibos);
        r.getCascada().put("multas", (long) multaRepository.findByAsociadoId(id).size());
        r.getCascada().put("lecturas", (long) lecturaRepository.findByAsociadoIdOrderByFechaLecturaDesc(id).size());
        r.getCascada().put("movimientosTesoreria", (long) movimientoTesoreriaRepository.findByAsociadoId(id).size());
        long notificaciones = 0;
        if (usuarioRepository.findByAsociadoId(id).isPresent()) {
            Long uid = usuarioRepository.findByAsociadoId(id).get().getId();
            notificaciones = notificacionRepository.findByAutorId(uid).size() + notificacionRepository.findByDestinatarioId(uid).size();
            r.getCascada().put("cuentaVinculada", 1L);
        } else {
            r.getCascada().put("cuentaVinculada", 0L);
        }
        r.getCascada().put("notificaciones", notificaciones);
        r.getCascada().put("medidorDesvinculado", a.getMedidor() != null ? 1L : 0L);
        return r;
    }

    private VerificacionBorradoResponse verificarCuenta(Long id, String usernameActual) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada con id " + id));
        if (u.getUsername().equalsIgnoreCase(usernameActual)) {
            return VerificacionBorradoResponse.bloqueda("CUENTA", id, u.getUsername(),
                    List.of("Es la cuenta de la sesion actual; no se puede eliminar mientras se esta usando."));
        }
        VerificacionBorradoResponse r = VerificacionBorradoResponse.borrable("CUENTA", id, u.getUsername(),
                "Se borrara la cuenta y, en cascada, todo lo que le pertenece (publicaciones, formularios, notificaciones, conversaciones, pagos registrados, movimientos).");
        r.getCascada().put("publicaciones", (long) publicacionRepository.findByAutorId(id).size());
        r.getCascada().put("formularios", (long) encuestaRepository.findByAutorId(id).size());
        r.getCascada().put("notificaciones", (long) (notificacionRepository.findByAutorId(id).size()
                + notificacionRepository.findByDestinatarioId(id).size()));
        r.getCascada().put("registrosLectura", (long) notificacionLecturaRepository.findByUsuarioId(id).size());
        r.getCascada().put("respuestasDesvinculadas", (long) respuestaEncuestaRepository.findByUsuarioId(id).size());
        r.getCascada().put("pagosRegistrados", (long) pagoRepository.findByTesoreroId(id).size());
        r.getCascada().put("movimientosTesoreria", (long) movimientoTesoreriaRepository.findByUsuarioId(id).size());
        long conversaciones = conversacionRepository.findByUsuario1IdOrUsuario2Id(id, id).size();
        r.getCascada().put("conversacionesChat", conversaciones);
        return r;
    }

    private VerificacionBorradoResponse verificarPeriodo(Long id) {
        MesContable mes = mesContableRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Periodo contable no encontrado con id " + id));
        VerificacionBorradoResponse r = VerificacionBorradoResponse.borrable("PERIODO_CONTABLE", id,
                mes.getAnioContable().getAnio() + "-" + mes.getNumeroMes(),
                "Se borrara el periodo contable con todo lo que contenga (facturas, lecturas y movimientos del mes).");
        r.getCascada().put("facturas", (long) facturaRepository.findByMesContableId(id).size());
        r.getCascada().put("lecturas", (long) lecturaRepository.findByMesContableId(id).size());
        r.getCascada().put("movimientosTesoreria", (long) movimientoTesoreriaRepository.findByMesContableId(id).size());
        return r;
    }

    private VerificacionBorradoResponse verificarAnio(Long id) {
        AnioContable anio = anioContableRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Anio contable no encontrado con id " + id));
        VerificacionBorradoResponse r = VerificacionBorradoResponse.borrable("ANIO_CONTABLE", id, String.valueOf(anio.getAnio()),
                "Se borrara el anio contable y, en cascada, todos sus meses con todo lo que contengan.");
        long meses = mesContableRepository.findByAnioContableIdOrderByNumeroMes(anio.getId()).size();
        long facturas = 0, lecturas = 0, movimientos = 0;
        for (MesContable mes : mesContableRepository.findByAnioContableIdOrderByNumeroMes(anio.getId())) {
            facturas += facturaRepository.findByMesContableId(mes.getId()).size();
            lecturas += lecturaRepository.findByMesContableId(mes.getId()).size();
            movimientos += movimientoTesoreriaRepository.findByMesContableId(mes.getId()).size();
        }
        r.getCascada().put("meses", meses);
        r.getCascada().put("facturas", facturas);
        r.getCascada().put("lecturas", lecturas);
        r.getCascada().put("movimientosTesoreria", movimientos);
        return r;
    }

    private VerificacionBorradoResponse verificarMulta(Long id) {
        Multa multa = multaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Multa no encontrada con id " + id));
        VerificacionBorradoResponse r = VerificacionBorradoResponse.borrable("MULTA", id, multa.getMotivo(),
                multa.getFactura() != null
                        ? "Se borrara la multa y se descontara su valor del total de la factura a la que estaba incluida."
                        : "Se borrara la multa. No afecta ninguna factura.");
        r.getCascada().put("facturaRecalculada", multa.getFactura() != null ? 1L : 0L);
        return r;
    }

    private VerificacionBorradoResponse verificarMedidor(Long id) {
        Medidor medidor = medidorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Medidor no encontrado con id " + id));
        VerificacionBorradoResponse r = VerificacionBorradoResponse.borrable("MEDIDOR", id, medidor.getNumero(),
                "Se borrara el medidor. Si estaba vinculado a un asociado, solo se desvincula (el asociado NO se borra). Sus lecturas se borran en cascada.");
        r.getCascada().put("lecturas", (long) lecturaRepository.findByMedidorId(id).size());
        r.getCascada().put("asociadoDesvinculado", medidor.getAsociado() != null ? 1L : 0L);
        return r;
    }

    private VerificacionBorradoResponse verificarLectura(Long id) {
        Lectura lectura = lecturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lectura no encontrada con id " + id));
        VerificacionBorradoResponse r = VerificacionBorradoResponse.borrable("LECTURA", id,
                "Medidor " + lectura.getMedidor().getNumero(),
                "Se borrara la lectura. Si genero factura, la factura se borra en cascada.");
        r.getCascada().put("facturaEliminada", facturaRepository.findByLecturaId(id).isPresent() ? 1L : 0L);
        return r;
    }

    private VerificacionBorradoResponse verificarPago(Long id) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado con id " + id));
        VerificacionBorradoResponse r = VerificacionBorradoResponse.borrable("PAGO", id,
                pago.getFactura() != null ? pago.getFactura().getNumeroFactura() : "Multa independiente",
                "Se borrara el pago con su recibo y movimientos, y se recalculara el estado de la factura.");
        r.getCascada().put("recibo", reciboRepository.findByPagoId(id).isPresent() ? 1L : 0L);
        r.getCascada().put("movimientosTesoreria", reciboRepository.findByPagoId(id)
                .map(rec -> (long) movimientoTesoreriaRepository.findByReciboId(rec.getId()).size())
                .orElse(0L));
        r.getCascada().put("facturaRecalculada", 1L);
        return r;
    }

    private VerificacionBorradoResponse verificarMovimiento(Long id) {
        MovimientoTesoreria movimiento = movimientoTesoreriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Movimiento no encontrado con id " + id));
        if (movimiento.getRecibo() != null) {
            VerificacionBorradoResponse r = VerificacionBorradoResponse.borrable("MOVIMIENTO", id, movimiento.getId().toString(),
                    "Es una entrada de un pago: se borrara en cascada el pago, su recibo y se recalculara la factura.");
            r.getCascada().put("pago", 1L);
            r.getCascada().put("recibo", 1L);
            r.getCascada().put("facturaRecalculada", 1L);
            return r;
        }
        return VerificacionBorradoResponse.borrable("MOVIMIENTO", id, movimiento.getId().toString(),
                "Se borrara el movimiento de tesoreria sin afectar otras entidades.");
    }

    private VerificacionBorradoResponse verificarNotificacion(Long id) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificacion no encontrada con id " + id));
        VerificacionBorradoResponse r = VerificacionBorradoResponse.borrable("NOTIFICACION", id, notificacion.getTitulo(),
                "Se borrara la notificacion junto con sus registros de lectura.");
        r.getCascada().put("registrosLectura", (long) notificacionLecturaRepository.findByNotificacionId(id).size());
        return r;
    }
}