package com.acueducto.backend.config;

import com.acueducto.backend.entity.*;
import com.acueducto.backend.entity.enums.EstadoAnio;
import com.acueducto.backend.entity.enums.EstadoMes;
import com.acueducto.backend.entity.enums.Rol;
import com.acueducto.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Inicializa datos minimos para poder usar y probar la API desde el primer arranque:
 * usuario administrador, configuracion base, metodos de pago y el periodo contable vigente.
 * Solo se ejecuta si la base de datos esta vacia (no sobrescribe informacion existente).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatosInicialesLoader implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final AnioContableRepository anioContableRepository;
    private final MesContableRepository mesContableRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        crearConfiguracionInicial();
        crearMetodosPagoIniciales();
        crearAdministradorInicial();
        crearPeriodoContableActual();
    }

    private void crearConfiguracionInicial() {
        if (configuracionRepository.count() > 0) return;

        Configuracion config = Configuracion.builder()
                .nombreAcueducto("Acueducto Los Guaduales")
                .direccion("Vereda Los Guaduales")
                .telefonoPrincipal("000-000-0000")
                .correo("contacto@acueductolosguaduales.example.com")
                .municipio("Municipio")
                .departamento("Departamento")
                .valorM3(BigDecimal.valueOf(1500))
                .cargoFijoAdministracion(BigDecimal.valueOf(8000))
                .valorReconexion(BigDecimal.valueOf(15000))
                .valorMultaDefecto(BigDecimal.valueOf(5000))
                .diasPlazoPago(15)
                .siguienteNumeroFactura(1L)
                .siguienteNumeroRecibo(1L)
                .siguienteNumeroEntrada(1L)
                .siguienteNumeroSalida(1L)
                .notasFactura("Gracias por su pago oportuno.")
                .build();
        configuracionRepository.save(config);
        log.info("Configuracion inicial creada. Ajuste los valores reales desde el modulo de Configuracion.");
    }

    private void crearMetodosPagoIniciales() {
        if (metodoPagoRepository.count() > 0) return;
        List<String> metodos = List.of("Efectivo", "Transferencia bancaria", "Consignacion bancaria", "Nequi", "Daviplata");
        metodos.forEach(nombre -> metodoPagoRepository.save(MetodoPago.builder().nombre(nombre).activo(true).build()));
    }

    private void crearAdministradorInicial() {
        if (usuarioRepository.existsByUsernameIgnoreCase("admin")) return;

        Usuario admin = Usuario.builder()
                .username("admin")
                .password(passwordEncoder.encode("Admin#2026"))
                .email("admin@acueductolosguaduales.example.com")
                .rol(Rol.ADMINISTRADOR)
                .activo(true)
                .build();
        usuarioRepository.save(admin);
        log.warn("Se creo el usuario administrador por defecto (username: admin / password: Admin#2026). "
                + "Cambie esta contrasena inmediatamente en produccion.");
    }

    private void crearPeriodoContableActual() {
        int anioActual = LocalDate.now().getYear();
        AnioContable anio = anioContableRepository.findByAnio(anioActual)
                .orElseGet(() -> anioContableRepository.save(
                        AnioContable.builder().anio(anioActual).estado(EstadoAnio.ACTIVO).build()));

        int mesActual = LocalDate.now().getMonthValue();
        if (mesContableRepository.findByNumeroMesAndAnioContableId(mesActual, anio.getId()).isEmpty()) {
            String nombreMes = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
            mesContableRepository.save(MesContable.builder()
                    .nombreMes(nombreMes)
                    .numeroMes(mesActual)
                    .anioContable(anio)
                    .estado(EstadoMes.ABIERTO)
                    .fechaApertura(LocalDate.now())
                    .build());
        }
    }
}
