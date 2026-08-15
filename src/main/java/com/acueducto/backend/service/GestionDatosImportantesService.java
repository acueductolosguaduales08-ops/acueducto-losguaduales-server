package com.acueducto.backend.service;

import com.acueducto.backend.entity.*;
import com.acueducto.backend.exception.ReglaNegocioException;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modulo "Gestion de datos importantes" (punto 5 del pedido): eliminacion DEFINITIVA (borrado
 * fisico, irreversible) de formularios, facturas, recibos, asociados, cuentas, periodos
 * contables y multas. Exclusivo del Administrador.
 *
 * Cada operacion:
 * 1. Reconfirma la contrasena del Administrador (verificarPassword), aunque ya tenga sesion
 *    iniciada — es la misma logica que usa AuthService para listar cuentas (8).
 * 2. Verifica que borrar el registro no rompa datos relacionados importantes (ver el javadoc
 *    de cada metodo). Esto es una decision de diseno mia, no un pedido explicito: el propio
 *    proyecto ya tiene una regla equivalente para Asociado ("nunca se elimina fisicamente si
 *    tiene historial"); aplique el mismo criterio de proteccion a los demas tipos, en vez de
 *    permitir un borrado incondicional que podria romper la integridad de facturacion/pagos ya
 *    registrados. Cuando el borrado esta bloqueado, la excepcion explica por que y que hacer.
 * 3. Registra el borrado en auditoria (incluye quien lo hizo, aunque auditoriaService.registrar
 *    ya lo hace via el usuario autenticado a nivel de peticion).
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

    /** Borra el formulario y, en cascada, todas sus preguntas y las respuestas ya recibidas. */
    @Transactional
    public void eliminarFormulario(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Encuesta encuesta = encuestaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Formulario no encontrado con id " + id));

        // RespuestaEncuesta no cascada automaticamente desde Encuesta (solo preguntas si); se
        // borra a mano primero. Cada RespuestaEncuesta si cascada a sus RespuestaPregunta.
        respuestaEncuestaRepository.deleteAll(respuestaEncuestaRepository.findByEncuestaId(id));

        String titulo = encuesta.getTitulo();
        encuestaRepository.delete(encuesta); // cascada: PreguntaEncuesta (orphanRemoval)

        auditoriaService.registrar("ELIMINAR_FORMULARIO_DEFINITIVO", "DATOS_IMPORTANTES", titulo, "id=" + id);
    }

    /**
     * Borra la factura. Bloqueada si tiene pagos registrados (serian datos financieros reales
     * que se perderian). Si estaba ligada a una lectura, esa lectura vuelve a quedar disponible
     * para facturar (facturaGenerada=false). Las multas que apuntaban a esta factura no se
     * borran: quedan sueltas (factura=null), disponibles para incluirse en la proxima factura.
     */
    @Transactional
    public void eliminarFactura(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada con id " + id));

        if (!pagoRepository.findByFacturaId(id).isEmpty()) {
            throw new ReglaNegocioException(
                    "Esta factura tiene pagos registrados y no se puede eliminar definitivamente (se perderia el registro del dinero recibido).");
        }

        multaRepository.findByFacturaId(id).forEach(m -> {
            m.setFactura(null);
            multaRepository.save(m);
        });

        if (factura.getLectura() != null) {
            Lectura lectura = factura.getLectura();
            lectura.setFacturaGenerada(false);
            lecturaRepository.save(lectura);
        }

        String numero = factura.getNumeroFactura();
        facturaRepository.delete(factura);
        auditoriaService.registrar("ELIMINAR_FACTURA_DEFINITIVA", "DATOS_IMPORTANTES", numero, "id=" + id);
    }

    /**
     * Borra el recibo. No hay otro registro que apunte a un recibo, asi que no rompe nada a
     * nivel de base de datos; pero OJO: no revierte el estado/total pagado de la factura ni del
     * pago asociados, que quedan tal cual estaban. Es una eliminacion de constancia en papel,
     * no una reversa contable.
     */
    @Transactional
    public void eliminarRecibo(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Recibo recibo = reciboRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recibo no encontrado con id " + id));
        String numero = recibo.getNumeroRecibo();
        reciboRepository.delete(recibo);
        auditoriaService.registrar("ELIMINAR_RECIBO_DEFINITIVO", "DATOS_IMPORTANTES", numero, "id=" + id);
    }

    /**
     * Borra el asociado. Bloqueado si tiene historial (facturas, pagos o lecturas registradas)
     * — mismo criterio que ya usa AsociadoService.archivar(), llevado a un borrado real: si
     * tiene historial, use archivar en su lugar (baja logica, sin perder el historial). Tambien
     * bloqueado si tiene una cuenta de usuario vinculada (eliminela primero, por separado). Si
     * tiene un medidor asignado, el medidor se desvincula (no se borra) y queda disponible.
     */
    @Transactional
    public void eliminarAsociado(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Asociado asociado = asociadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asociado no encontrado con id " + id));

        boolean tieneHistorial = !facturaRepository.findByAsociadoId(id, Pageable.unpaged()).isEmpty()
                || !pagoRepository.findByAsociadoId(id).isEmpty()
                || !lecturaRepository.findByAsociadoIdOrderByFechaLecturaDesc(id).isEmpty();
        if (tieneHistorial) {
            throw new ReglaNegocioException(
                    "Este asociado tiene historial (facturas, pagos o lecturas) y no se puede eliminar definitivamente; use 'archivar' en su lugar.");
        }
        if (usuarioRepository.existsByAsociadoId(id)) {
            throw new ReglaNegocioException("Este asociado tiene una cuenta de usuario vinculada; elimine primero la cuenta.");
        }

        if (asociado.getMedidor() != null) {
            Medidor medidor = asociado.getMedidor();
            medidor.setAsociado(null);
            medidorRepository.save(medidor);
        }

        String codigo = asociado.getCodigoInterno();
        asociadoRepository.delete(asociado);
        auditoriaService.registrar("ELIMINAR_ASOCIADO_DEFINITIVO", "DATOS_IMPORTANTES", codigo, "id=" + id);
    }

    /**
     * Borra una cuenta de usuario. Bloqueada si es autor de algun formulario (quedaria un
     * formulario sin autor), y bloqueada para que el Administrador no pueda borrar su propia
     * cuenta por accidente mientras la esta usando.
     */
    @Transactional
    public void eliminarCuenta(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada con id " + id));

        if (usuario.getUsername().equalsIgnoreCase(usernameAdmin)) {
            throw new ReglaNegocioException("No puede eliminar su propia cuenta mientras la esta usando.");
        }
        if (encuestaRepository.existsByAutorId(id)) {
            throw new ReglaNegocioException("Esta cuenta es autora de al menos un formulario y no se puede eliminar.");
        }

        String username = usuario.getUsername();
        usuarioRepository.delete(usuario);
        auditoriaService.registrar("ELIMINAR_CUENTA_DEFINITIVA", "DATOS_IMPORTANTES", username, "id=" + id);
    }

    /** Borra un mes contable. Bloqueado si tiene lecturas o facturas registradas en ese mes. */
    @Transactional
    public void eliminarPeriodoContable(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        MesContable mes = mesContableRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Periodo contable no encontrado con id " + id));

        boolean enUso = !lecturaRepository.findByMesContableId(id).isEmpty()
                || !facturaRepository.findByMesContableId(id).isEmpty();
        if (enUso) {
            throw new ReglaNegocioException("Este periodo contable tiene lecturas o facturas registradas y no se puede eliminar.");
        }

        mesContableRepository.delete(mes);
        auditoriaService.registrar("ELIMINAR_PERIODO_CONTABLE_DEFINITIVO", "DATOS_IMPORTANTES",
                mes.getAnioContable().getAnio() + "-" + mes.getNumeroMes(), "id=" + id);
    }

    /** Borra una multa (7). Bloqueada si ya quedo incluida en una factura (protege el total ya calculado de esa factura). */
    @Transactional
    public void eliminarMulta(Long id, String usernameAdmin, String password) {
        authService.verificarPassword(usernameAdmin, password);
        Multa multa = multaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Multa no encontrada con id " + id));

        if (multa.getFactura() != null) {
            throw new ReglaNegocioException("Esta multa ya quedo incluida en una factura y no se puede eliminar.");
        }

        String motivo = multa.getMotivo();
        multaRepository.delete(multa);
        auditoriaService.registrar("ELIMINAR_MULTA_DEFINITIVA", "DATOS_IMPORTANTES", motivo, "id=" + id);
    }
}
