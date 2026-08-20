package com.acueducto.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runner de soporte para generar el archivo de clases compartidas (AppCDS) durante el build de
 * la imagen Docker. Es INERTE en produccion: solo ejecuta System.exit(0) cuando la JVM se lanza
 * con la propiedad {@code -Dapp.cds-dump=true}, que unicamente se usa en el paso de entrenamiento
 * del Dockerfile para que la JVM termine de forma ordenada y escriba el archivo {@code app.jsa}
 * (via -XX:ArchiveClassesAtExit). Sin esa propiedad este runner no hace absolutamente nada.
 *
 * <p>Se ejecuta con la prioridad mas baja posible para que corra despues de todos los demas
 * runners (DatosInicialesLoader, ConfiguracionSchemaMigrator) y asi la mayoria de las clases de
 * la aplicacion ya esten cargadas cuando se hace el dump.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class CdsExitRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        if (Boolean.getBoolean("app.cds-dump")) {
            System.exit(0);
        }
    }
}