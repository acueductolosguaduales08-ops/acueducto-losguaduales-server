package com.acueducto.backend.service;

import com.acueducto.backend.dto.request.MovimientoTesoreriaRequest;
import com.acueducto.backend.dto.request.MultaRequest;
import com.acueducto.backend.dto.request.RegistrarPagoRequest;
import com.acueducto.backend.dto.response.*;
import com.acueducto.backend.entity.*;
import com.acueducto.backend.entity.enums.*;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.exception.ReglaNegocioException;
import com.acueducto.backend.repository.*;
import com.acueducto.backend.security.UserPrincipal;
import com.acueducto.backend.util.NumeracionUtil;
import com.acueducto.backend.util.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Modulo de Tesoreria (Modulo 8). Administra pagos, recibos, multas e ingresos/egresos.
 * El registro de un pago es una operacion atomica que actualiza la factura, crea el
 * movimiento financiero, genera el recibo y dispara la notificacion correspondiente (4.11 / 8.5).
 */
@Service
@RequiredArgsConstructor
public class TesoreriaService {

    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;
    private final ReciboRepository reciboRepository;
    private final MultaRepository multaRepository;
    private final MovimientoTesoreriaRepository movimientoTesoreriaRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final MesContableRepository mesContableRepository;
    private final AsociadoRepository asociadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionService configuracionService;
    private final QrCodeService qrCodeService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;

    /** Pasos 1-9 de la seccion 8.5, ejecutados como una unica transaccion. */
    @Transactional
    public PagoResponse registrarPago(RegistrarPagoRequest request) {
        Factura factura = facturaRepository.findById(request.facturaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada"));

        if (factura.getEstado() == EstadoFactura.PAGADA) {
            throw new ReglaNegocioException("La factura ya se encuentra pagada en su totalidad.");
        }
        if (factura.getEstado() == EstadoFactura.ANULADA) {
            throw new ReglaNegocioException("No es posible registrar pagos sobre una factura anulada.");
        }
        if (factura.getMesContable().getEstado() == EstadoMes.CERRADO) {
            throw new ReglaNegocioException("El periodo contable de esta factura esta cerrado.");
        }

        BigDecimal saldoPendiente = factura.getSaldoPendiente();
        if (request.valor().compareTo(saldoPendiente) > 0) {
            throw new ReglaNegocioException(
                    "El valor del pago ($" + request.valor() + ") supera el saldo pendiente ($" + saldoPendiente + ").");
        }

        MetodoPago metodoPago = metodoPagoRepository.findById(request.metodoPagoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Metodo de pago no encontrado"));

        Usuario tesorero = usuarioActual();

        // 1-3: registrar el valor recibido
        Pago pago = Pago.builder()
                .factura(factura)
                .asociado(factura.getAsociado())
                .valor(request.valor())
                .fecha(LocalDateTime.now())
                .metodoPago(metodoPago)
                .tesorero(tesorero)
                .observaciones(request.observaciones())
                .anulado(false)
                .build();
        pago = pagoRepository.save(pago);

        // 4: actualizar el estado de la factura
        factura.setTotalPagado(factura.getTotalPagado().add(request.valor()));
        BigDecimal nuevoSaldo = factura.getSaldoPendiente();
        factura.setEstado(nuevoSaldo.compareTo(BigDecimal.ZERO) <= 0 ? EstadoFactura.PAGADA : EstadoFactura.PAGADA_PARCIAL);
        factura = facturaRepository.save(factura);

        // 5: crear el movimiento financiero (entrada)
        MovimientoTesoreria movimiento = crearMovimiento(TipoMovimiento.ENTRADA, request.valor(), metodoPago,
                "Pago factura " + factura.getNumeroFactura(), "PAGO_FACTURA",
                factura.getAsociado(), factura, null, factura.getMesContable(), tesorero, null);

        // 6: generar automaticamente el recibo
        long consecutivoRecibo = configuracionService.siguienteNumeroRecibo();
        String numeroRecibo = NumeracionUtil.formatearRecibo(consecutivoRecibo);

        Recibo recibo = Recibo.builder()
                .numeroRecibo(numeroRecibo)
                .pago(pago)
                .factura(factura)
                .asociado(factura.getAsociado())
                .fechaEmision(LocalDateTime.now())
                .valor(request.valor())
                .saldoPendiente(nuevoSaldo)
                .estado(EstadoRecibo.EMITIDO)
                .build();
        recibo = reciboRepository.save(recibo);
        recibo.setCodigoQr(qrCodeService.generarQrRecibo(recibo.getNumeroRecibo()));
        recibo = reciboRepository.save(recibo);

        movimiento.setRecibo(recibo);
        movimientoTesoreriaRepository.save(movimiento);

        // 7: auditoria
        auditoriaService.registrar("REGISTRAR_PAGO", "TESORERIA", factura.getNumeroFactura(),
                "Recibo " + recibo.getNumeroRecibo() + " - Valor $" + request.valor());

        // 9: notificacion
        notificacionService.notificarPagoRegistrado(recibo);

        return PagoResponse.fromEntity(pago, recibo.getNumeroRecibo());
    }

    @Transactional
    public MultaResponse registrarMulta(MultaRequest request) {
        Asociado asociado = asociadoRepository.findById(request.asociadoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Asociado no encontrado"));

        Factura factura = null;
        if (request.facturaId() != null) {
            factura = facturaRepository.findById(request.facturaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada"));
        }

        Multa multa = Multa.builder()
                .asociado(asociado)
                .factura(factura)
                .motivo(request.motivo())
                .valor(request.valor())
                .fecha(LocalDate.now())
                .estado(EstadoMulta.PENDIENTE)
                .build();
        multa = multaRepository.save(multa);

        if (factura != null && factura.getEstado() != EstadoFactura.PAGADA && factura.getEstado() != EstadoFactura.ANULADA) {
            factura.setTotalMultas(factura.getTotalMultas().add(request.valor()));
            factura.setTotal(factura.getTotal().add(request.valor()));
            facturaRepository.save(factura);
        }

        auditoriaService.registrar("REGISTRAR_MULTA", "TESORERIA", asociado.getDocumento(), request.motivo());
        return MultaResponse.fromEntity(multa);
    }

    /** Registra un ingreso extraordinario (donaciones, reconexiones, nuevas afiliaciones, etc.) (8.4). */
    @Transactional
    public MovimientoTesoreriaResponse registrarIngreso(MovimientoTesoreriaRequest request) {
        return registrarMovimiento(request, TipoMovimiento.ENTRADA);
    }

    /** Registra un gasto o egreso administrativo (8.9). */
    @Transactional
    public MovimientoTesoreriaResponse registrarGasto(MovimientoTesoreriaRequest request) {
        return registrarMovimiento(request, TipoMovimiento.SALIDA);
    }

    private MovimientoTesoreriaResponse registrarMovimiento(MovimientoTesoreriaRequest request, TipoMovimiento tipo) {
        MetodoPago metodoPago = metodoPagoRepository.findById(request.metodoPagoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Metodo de pago no encontrado"));
        MesContable mes = mesContableRepository.findById(request.mesContableId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Mes contable no encontrado"));
        if (mes.getEstado() == EstadoMes.CERRADO) {
            throw new ReglaNegocioException("No se pueden registrar movimientos en un periodo cerrado.");
        }

        Asociado asociado = request.asociadoId() != null
                ? asociadoRepository.findById(request.asociadoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Asociado no encontrado"))
                : null;

        Usuario usuario = usuarioActual();

        MovimientoTesoreria movimiento = crearMovimiento(tipo, request.valor(), metodoPago, request.concepto(),
                request.categoria(), asociado, null, null, mes, usuario, request.comprobanteUrl());
        movimiento.setObservaciones(request.observaciones());
        movimiento = movimientoTesoreriaRepository.save(movimiento);

        auditoriaService.registrar("REGISTRAR_" + tipo.name(), "TESORERIA", movimiento.getId().toString(), request.concepto());
        return MovimientoTesoreriaResponse.fromEntity(movimiento);
    }

    private MovimientoTesoreria crearMovimiento(TipoMovimiento tipo, BigDecimal valor, MetodoPago metodoPago,
                                                 String concepto, String categoria, Asociado asociado,
                                                 Factura factura, Recibo recibo, MesContable mes,
                                                 Usuario usuario, String comprobanteUrl) {
        long siguienteNumero = movimientoTesoreriaRepository.findTopByTipoOrderByNumeroDesc(tipo)
                .map(m -> m.getNumero() + 1).orElse(1L);

        MovimientoTesoreria movimiento = MovimientoTesoreria.builder()
                .tipo(tipo)
                .numero(siguienteNumero)
                .fecha(LocalDateTime.now())
                .valor(valor)
                .metodoPago(metodoPago)
                .concepto(concepto)
                .categoria(categoria)
                .asociado(asociado)
                .factura(factura)
                .recibo(recibo)
                .mesContable(mes)
                .usuario(usuario)
                .comprobanteUrl(comprobanteUrl)
                .anulado(false)
                .build();
        return movimientoTesoreriaRepository.save(movimiento);
    }

    @Transactional
    public void anularMovimiento(Long id, String motivo) {
        MovimientoTesoreria movimiento = movimientoTesoreriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Movimiento no encontrado"));
        movimiento.setAnulado(true);
        movimientoTesoreriaRepository.save(movimiento);
        auditoriaService.registrar("ANULAR_MOVIMIENTO", "TESORERIA", movimiento.getId().toString(), motivo);
    }

    public CajaDiariaResponse cajaDiaria() {
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = LocalDate.now().atTime(23, 59, 59);
        List<MovimientoTesoreria> movimientos = movimientoTesoreriaRepository.findByFechaBetweenAndAnuladoFalse(inicioDia, finDia);

        BigDecimal ingresos = movimientos.stream().filter(m -> m.getTipo() == TipoMovimiento.ENTRADA)
                .map(MovimientoTesoreria::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gastos = movimientos.stream().filter(m -> m.getTipo() == TipoMovimiento.SALIDA)
                .map(MovimientoTesoreria::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        return CajaDiariaResponse.builder()
                .totalIngresos(ingresos)
                .totalGastos(gastos)
                .balance(ingresos.subtract(gastos))
                .numeroMovimientos(movimientos.size())
                .build();
    }

    public Page<MovimientoTesoreriaResponse> listarMovimientos(TipoMovimiento tipo, Pageable pageable) {
        return movimientoTesoreriaRepository.findByTipo(tipo, pageable).map(MovimientoTesoreriaResponse::fromEntity);
    }

    /** Historial combinado de entradas y salidas, ordenado (ej: ?sort=fecha,desc). Util para una vista unica de movimientos. */
    public Page<MovimientoTesoreriaResponse> listarTodosLosMovimientos(Pageable pageable) {
        return movimientoTesoreriaRepository.findAll(pageable).map(MovimientoTesoreriaResponse::fromEntity);
    }

    public Page<ReciboResponse> listarRecibosPorAsociado(Long asociadoId, Pageable pageable) {
        return reciboRepository.findByAsociadoId(asociadoId, pageable).map(ReciboResponse::fromEntity);
    }

    public Recibo obtenerReciboPorNumero(String numeroRecibo) {
        return reciboRepository.findByNumeroRecibo(numeroRecibo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recibo no encontrado: " + numeroRecibo));
    }

    public List<MultaResponse> listarMultasPorAsociado(Long asociadoId) {
        return multaRepository.findByAsociadoId(asociadoId).stream().map(MultaResponse::fromEntity).toList();
    }

    private Usuario usuarioActual() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado"));
    }
}
