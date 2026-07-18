package com.acueducto.backend.service;

import com.acueducto.backend.dto.request.AsociadoRequest;
import com.acueducto.backend.dto.request.CambioEstadoServicioRequest;
import com.acueducto.backend.dto.response.AsociadoResponse;
import com.acueducto.backend.dto.response.AsociadoResumenFinancieroResponse;
import com.acueducto.backend.entity.Asociado;
import com.acueducto.backend.entity.Medidor;
import com.acueducto.backend.entity.enums.EstadoFactura;
import com.acueducto.backend.entity.enums.EstadoMulta;
import com.acueducto.backend.entity.enums.EstadoServicio;
import com.acueducto.backend.exception.RecursoDuplicadoException;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.exception.ReglaNegocioException;
import com.acueducto.backend.repository.AsociadoRepository;
import com.acueducto.backend.repository.FacturaRepository;
import com.acueducto.backend.repository.MedidorRepository;
import com.acueducto.backend.repository.MultaRepository;
import com.acueducto.backend.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Nucleo del sistema (Modulo 5): gestiona el ciclo de vida completo del asociado,
 * respetando las reglas de integridad que impiden eliminar historial administrativo (2.12 / 5.8).
 */
@Service
@RequiredArgsConstructor
public class AsociadoService {

    private final AsociadoRepository asociadoRepository;
    private final MedidorRepository medidorRepository;
    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;
    private final MultaRepository multaRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public AsociadoResponse crear(AsociadoRequest request) {
        if (asociadoRepository.existsByDocumento(request.documento())) {
            throw new RecursoDuplicadoException("Ya existe un asociado registrado con este documento.");
        }
        Medidor medidor = medidorRepository.findByNumero(request.numeroMedidor())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El medidor " + request.numeroMedidor() + " no existe. Registrelo primero en el modulo de medidores."));
        if (medidor.getAsociado() != null) {
            throw new ReglaNegocioException("El medidor ya se encuentra asignado a otro asociado.");
        }

        Asociado asociado = Asociado.builder()
                .tipoDocumento(request.tipoDocumento())
                .documento(request.documento())
                .nombres(request.nombres())
                .apellidos(request.apellidos())
                .fechaNacimiento(request.fechaNacimiento())
                .telefonoPrincipal(request.telefonoPrincipal())
                .telefonoAlternativo(request.telefonoAlternativo())
                .correo(request.correo())
                .direccion(request.direccion())
                .barrioVereda(request.barrioVereda())
                .observaciones(request.observaciones())
                .estadoServicio(EstadoServicio.ACTIVO)
                .fechaAfiliacion(request.fechaAfiliacion() != null ? request.fechaAfiliacion() : LocalDate.now())
                .archivado(false)
                .build();

        asociado = asociadoRepository.save(asociado);
        asociado.setCodigoInterno(com.acueducto.backend.util.NumeracionUtil.formatearCodigoAsociado(asociado.getId()));
        asociado = asociadoRepository.save(asociado);

        medidor.setAsociado(asociado);
        medidorRepository.save(medidor);

        auditoriaService.registrar("CREAR_ASOCIADO", "ASOCIADOS", asociado.getCodigoInterno(), null);
        return AsociadoResponse.fromEntity(asociado);
    }

    @Transactional
    public AsociadoResponse editar(Long id, AsociadoRequest request) {
        Asociado asociado = obtenerEntidad(id);

        asociado.setNombres(request.nombres());
        asociado.setApellidos(request.apellidos());
        asociado.setFechaNacimiento(request.fechaNacimiento());
        asociado.setTelefonoPrincipal(request.telefonoPrincipal());
        asociado.setTelefonoAlternativo(request.telefonoAlternativo());
        asociado.setCorreo(request.correo());
        asociado.setDireccion(request.direccion());
        asociado.setBarrioVereda(request.barrioVereda());
        asociado.setObservaciones(request.observaciones());

        // Reasignacion opcional de medidor, solo si no genera conflicto (5.6)
        if (request.numeroMedidor() != null
                && (asociado.getMedidor() == null || !asociado.getMedidor().getNumero().equals(request.numeroMedidor()))) {
            Medidor nuevoMedidor = medidorRepository.findByNumero(request.numeroMedidor())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Medidor no encontrado"));
            if (nuevoMedidor.getAsociado() != null && !nuevoMedidor.getAsociado().getId().equals(id)) {
                throw new ReglaNegocioException("El medidor ya esta asignado a otro asociado.");
            }
            if (asociado.getMedidor() != null) {
                asociado.getMedidor().setAsociado(null);
                medidorRepository.save(asociado.getMedidor());
            }
            nuevoMedidor.setAsociado(asociado);
            medidorRepository.save(nuevoMedidor);
        }

        asociado = asociadoRepository.save(asociado);
        auditoriaService.registrar("EDITAR_ASOCIADO", "ASOCIADOS", asociado.getCodigoInterno(), null);
        return AsociadoResponse.fromEntity(asociado);
    }

    @Transactional
    public AsociadoResponse cambiarEstadoServicio(Long id, CambioEstadoServicioRequest request) {
        Asociado asociado = obtenerEntidad(id);
        EstadoServicio anterior = asociado.getEstadoServicio();
        asociado.setEstadoServicio(request.estado());

        if (request.estado() == EstadoServicio.SUSPENDIDO) {
            asociado.setFechaSuspension(LocalDate.now());
            asociado.setMotivoSuspension(request.motivo());
        } else if (request.estado() == EstadoServicio.ACTIVO && anterior == EstadoServicio.SUSPENDIDO) {
            asociado.setFechaReactivacion(LocalDate.now());
        }

        asociado = asociadoRepository.save(asociado);
        auditoriaService.registrar("CAMBIO_ESTADO_SERVICIO", "ASOCIADOS", asociado.getCodigoInterno(),
                anterior + " -> " + request.estado());
        return AsociadoResponse.fromEntity(asociado);
    }

    /** Baja logica: nunca se elimina fisicamente si tiene historial (2.12 / 5.8). */
    @Transactional
    public void archivar(Long id) {
        Asociado asociado = obtenerEntidad(id);
        boolean tieneHistorial = !facturaRepository.findByAsociadoId(id, org.springframework.data.domain.Pageable.unpaged()).isEmpty()
                || !pagoRepository.findByAsociadoId(id).isEmpty();

        asociado.setArchivado(true);
        asociado.setEstadoServicio(EstadoServicio.INACTIVO);
        asociadoRepository.save(asociado);

        auditoriaService.registrar("ARCHIVAR_ASOCIADO", "ASOCIADOS", asociado.getCodigoInterno(),
                tieneHistorial ? "Archivado logico (posee historial)" : "Archivado logico");
    }

    public AsociadoResponse obtener(Long id) {
        return AsociadoResponse.fromEntity(obtenerEntidad(id));
    }

    public Asociado obtenerEntidad(Long id) {
        return asociadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asociado no encontrado con id " + id));
    }

    public List<AsociadoResponse> buscar(String query) {
        if (query == null || query.isBlank()) {
            return asociadoRepository.findAll().stream()
                    .filter(a -> !a.isArchivado())
                    .map(AsociadoResponse::fromEntity).toList();
        }
        return asociadoRepository.buscar(query).stream().map(AsociadoResponse::fromEntity).toList();
    }

    public List<AsociadoResponse> listarPorEstado(EstadoServicio estado) {
        return asociadoRepository.findByEstadoServicioAndArchivadoFalse(estado).stream()
                .map(AsociadoResponse::fromEntity).toList();
    }

    /** Informacion financiera calculada dinamicamente, nunca almacenada (5.4). */
    public AsociadoResumenFinancieroResponse resumenFinanciero(Long asociadoId) {
        var facturas = facturaRepository.findByAsociadoId(asociadoId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        long pagadas = facturas.stream().filter(f -> f.getEstado() == EstadoFactura.PAGADA).count();
        long pendientes = facturas.stream().filter(f -> f.getEstado() == EstadoFactura.PENDIENTE
                || f.getEstado() == EstadoFactura.PAGADA_PARCIAL || f.getEstado() == EstadoFactura.VENCIDA).count();
        BigDecimal totalPagado = facturas.stream().map(com.acueducto.backend.entity.Factura::getTotalPagado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPendiente = facturas.stream().map(com.acueducto.backend.entity.Factura::getSaldoPendiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var multas = multaRepository.findByAsociadoId(asociadoId);
        long numeroMultas = multas.size();
        BigDecimal totalMultas = multas.stream()
                .filter(m -> m.getEstado() != EstadoMulta.ANULADA)
                .map(com.acueducto.backend.entity.Multa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AsociadoResumenFinancieroResponse.builder()
                .totalFacturas(facturas.size())
                .facturasPagadas(pagadas)
                .facturasPendientes(pendientes)
                .totalPagado(totalPagado)
                .totalPendiente(totalPendiente)
                .numeroMultas(numeroMultas)
                .totalMultas(totalMultas)
                .build();
    }
}
