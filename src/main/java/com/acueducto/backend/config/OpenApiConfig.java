package com.acueducto.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de Swagger / OpenAPI. La UI queda disponible en /swagger-ui.html
 * y el JSON de la especificacion en /api-docs. Permite probar todos los endpoints,
 * incluyendo los protegidos, usando el boton "Authorize" con un Bearer token JWT.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API - Sistema Integral de Gestion del Acueducto Los Guaduales",
                version = "1.0.0",
                description = """
                        API REST para la gestion de asociados, medidores, facturacion, tesoreria,
                        periodos contables, encuestas, notificaciones y el portal publico del
                        Acueducto Los Guaduales.

                        Roles disponibles: ASOCIADO, TESORERO, ADMINISTRADOR (ademas del acceso publico sin login).

                        Para probar los endpoints protegidos:
                        1. Ejecute POST /api/v1/auth/login con sus credenciales.
                        2. Copie el 'accessToken' de la respuesta.
                        3. Haga clic en 'Authorize' (arriba a la derecha) y pegue el token (sin la palabra Bearer).
                        """,
                contact = @Contact(name = "Acueducto Los Guaduales", email = "sistemas@acueductolosguaduales.com")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Servidor local"),
                @Server(url = "https://acueducto-losguaduales-server.onrender.com", description = "Servidor Render")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Ingrese el token JWT obtenido en /api/v1/auth/login"
)
public class OpenApiConfig {
}
