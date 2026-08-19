package com.acueducto.backend.service;

import com.acueducto.backend.dto.request.MedidorRequest;
import com.acueducto.backend.dto.response.MedidorResponse;
import com.acueducto.backend.entity.Asociado;
import com.acueducto.backend.entity.Medidor;
import com.acueducto.backend.entity.enums.EstadoMedidor;
import com.acueducto.backend.exception.RecursoDuplicadoException;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.exception.ReglaNegocioException;
import com.acueducto.backend.repository.AsociadoRepository;
import com.acueducto.backend.repository.MedidorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedidorService {

    private final MedidorRepository medidorRepository;
    private final AsociadoRepository asociadoRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public MedidorResponse crear(MedidorRequest request) {
        if (medidorRepository.existsByNumero(request.numero())) {
            throw new RecursoDuplicadoException("Ya existe un medidor con el numero " + request.numero());
        }

        Asociado asociado = null;
        if (request.asociadoId() != null) {
            asociado = asociadoRepository.findById(request.asociadoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Asociado no encontrado"));
            if (asociado.getMedidor() != null) {
                throw new ReglaNegocioException("El asociado ya tiene un medidor activo asignado.");
            }
        }

        Medidor medidor = Medidor.builder()
                .numero(request.numero())
                .asociado(asociado)
                .fechaInstalacion(request.fechaInstalacion())
                .estado(EstadoMedidor.ACTIVO)
                .ubicacion(request.ubicacion())
                .observaciones(request.observaciones())
                .build();

        medidor = medidorRepository.save(medidor);
        medidor.setCodigoInterno("MED-" + String.format("%05d", medidor.getId()));
        medidor = medidorRepository.save(medidor);

        auditoriaService.registrar("CREAR_MEDIDOR", "MEDIDORES", medidor.getNumero(), null);
        return MedidorResponse.fromEntity(medidor);
    }

    @Transactional
    public MedidorResponse editar(Long id, MedidorRequest request) {
        Medidor medidor = obtenerEntidad(id);
        medidor.setUbicacion(request.ubicacion());
        medidor.setObservaciones(request.observaciones());
        medidor.setFechaInstalacion(request.fechaInstalacion());
        medidor = medidorRepository.save(medidor);
        auditoriaService.registrar("EDITAR_MEDIDOR", "MEDIDORES", medidor.getNumero(), null);
        return MedidorResponse.fromEntity(medidor);
    }

    @Transactional
    public MedidorResponse cambiarEstado(Long id, EstadoMedidor estado) {
        Medidor medidor = obtenerEntidad(id);
        medidor.setEstado(estado);
        medidor = medidorRepository.save(medidor);
        auditoriaService.registrar("CAMBIO_ESTADO_MEDIDOR", "MEDIDORES", medidor.getNumero(), estado.name());
        return MedidorResponse.fromEntity(medidor);
    }

    public MedidorResponse obtener(Long id) {
        return MedidorResponse.fromEntity(obtenerEntidad(id));
    }

    public Medidor obtenerEntidad(Long id) {
        return medidorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Medidor no encontrado con id " + id));
    }

    public List<MedidorResponse> listar() {
        return medidorRepository.findAll().stream().map(MedidorResponse::fromEntity).toList();
    }
}
