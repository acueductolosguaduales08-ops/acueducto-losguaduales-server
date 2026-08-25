package com.acueducto.backend.service;

import com.acueducto.backend.dto.response.DeudaResponse;
import com.acueducto.backend.entity.Factura;
import com.acueducto.backend.entity.enums.EstadoFactura;
import com.acueducto.backend.repository.FacturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeudaService {

    private final FacturaRepository facturaRepository;

    public DeudaResponse obtenerCartera() {
        LocalDate hoy = LocalDate.now();
        List<Factura> vencidas = facturaRepository.findByEstadoAndFechaLimitePagoBefore(
                EstadoFactura.PENDIENTE, hoy);
        List<Factura> parciales = facturaRepository.findByEstado(EstadoFactura.PAGADA_PARCIAL);
        parciales.removeIf(f -> f.getSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0);

        List<Factura> todas = new ArrayList<>(vencidas);
        todas.addAll(parciales);

        BigDecimal montoTotal = BigDecimal.ZERO;
        List<DeudaResponse.DetalleDeuda> detalle = new ArrayList<>();

        for (Factura f : todas) {
            BigDecimal saldo = f.getSaldoPendiente();
            if (saldo.compareTo(BigDecimal.ZERO) <= 0) continue;

            montoTotal = montoTotal.add(saldo);
            long diasVencidos = ChronoUnit.DAYS.between(f.getFechaLimitePago(), hoy);
            if (diasVencidos < 0) diasVencidos = 0;

            String rangoAging = clasificarAging(diasVencidos);

            detalle.add(DeudaResponse.DetalleDeuda.builder()
                    .facturaId(f.getId())
                    .numeroFactura(f.getNumeroFactura())
                    .asociadoId(f.getAsociado().getId())
                    .asociadoNombre(f.getAsociado().getNombres() + " " + f.getAsociado().getApellidos())
                    .numeroMedidor(f.getAsociado().getMedidor() != null ? f.getAsociado().getMedidor().getNumero() : null)
                    .total(f.getTotal())
                    .totalPagado(f.getTotalPagado())
                    .saldoPendiente(saldo)
                    .fechaLimitePago(f.getFechaLimitePago().toString())
                    .diasVencimiento(diasVencidos)
                    .rangoAging(rangoAging)
                    .build());
        }

        List<DeudaResponse.GrupoAging> aging = new ArrayList<>();
        String[] rangos = {"Vigente", "1-30 días", "31-60 días", "61-90 días", "90+ días"};
        for (String rango : rangos) {
            long cantidad = detalle.stream().filter(d -> d.getRangoAging().equals(rango)).count();
            BigDecimal monto = detalle.stream()
                    .filter(d -> d.getRangoAging().equals(rango))
                    .map(DeudaResponse.DetalleDeuda::getSaldoPendiente)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            aging.add(DeudaResponse.GrupoAging.builder()
                    .rango(rango)
                    .cantidad(cantidad)
                    .monto(monto)
                    .build());
        }

        return DeudaResponse.builder()
                .totalFacturasVencidas(detalle.size())
                .montoTotalVencido(montoTotal)
                .aging(aging)
                .detalle(detalle)
                .build();
    }

    private String clasificarAging(long dias) {
        if (dias <= 0) return "Vigente";
        if (dias <= 30) return "1-30 días";
        if (dias <= 60) return "31-60 días";
        if (dias <= 90) return "61-90 días";
        return "90+ días";
    }
}
