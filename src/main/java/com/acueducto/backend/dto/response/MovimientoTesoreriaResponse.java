package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.MovimientoTesoreria;
import com.acueducto.backend.entity.enums.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoTesoreriaResponse {
    private Long id;
    private TipoMovimiento tipo;
    private String numeroFormateado;
    private LocalDateTime fecha;
    private BigDecimal valor;
    private String concepto;
    private String categoria;
    private String usuario;
    private String facturaNumero;
    private String reciboNumero;

    public static MovimientoTesoreriaResponse fromEntity(MovimientoTesoreria m) {
        String prefijo = m.getTipo() == TipoMovimiento.ENTRADA ? "ENT-" : "SAL-";
        return MovimientoTesoreriaResponse.builder()
                .id(m.getId())
                .tipo(m.getTipo())
                .numeroFormateado(prefijo + String.format("%06d", m.getNumero()))
                .fecha(m.getFecha())
                .valor(m.getValor())
                .concepto(m.getConcepto())
                .categoria(m.getCategoria())
                .usuario(m.getUsuario().getUsername())
                .facturaNumero(m.getFactura() != null ? m.getFactura().getNumeroFactura() : null)
                .reciboNumero(m.getRecibo() != null ? m.getRecibo().getNumeroRecibo() : null)
                .build();
    }
}
