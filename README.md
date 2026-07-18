# API Backend — Sistema Integral de Gestion del Acueducto Los Guaduales

Backend REST construido en **Java 17 + Spring Boot 3** para la gestion integral del
acueducto: asociados, medidores y consumos, facturacion, tesoreria, periodos contables,
encuestas/formularios, notificaciones y el portal publico.

## Stack tecnico

| Capa | Tecnologia |
|---|---|
| Lenguaje / Framework | Java 17, Spring Boot 3.3 |
| Seguridad | Spring Security + JWT (`jjwt`), roles `ASOCIADO` / `TESORERO` / `ADMINISTRADOR` |
| Persistencia | Spring Data JPA + Hibernate. PostgreSQL en produccion, H2 en memoria para desarrollo |
| Documentacion API | springdoc-openapi (Swagger UI) |
| PDF | Thymeleaf (plantillas HTML) + Flying Saucer / OpenPDF (conversion HTML → PDF pixel-perfect) |
| Codigos QR | ZXing (Zebra Crossing) |
| Otros | Bean Validation, Lombok, Commons Lang3 |

## Arquitectura del proyecto

```
src/main/java/com/acueducto/backend/
├── config/          Seguridad, CORS, OpenAPI/Swagger, JPA Auditing, carga de datos iniciales
├── security/        JWT (generacion/validacion), filtro de autenticacion, UserDetailsService
├── entity/          Entidades JPA (Asociado, Medidor, Lectura, Factura, Pago, Recibo, ...)
│   └── enums/        Enumeraciones del dominio
├── repository/      Repositorios Spring Data JPA
├── service/         Logica de negocio (una clase por modulo funcional)
├── controller/       Controladores REST + documentacion Swagger
├── dto/
│   ├── request/      DTOs de entrada (validados con Bean Validation)
│   └── response/     DTOs de salida
├── exception/        Excepciones de negocio + manejador global (@RestControllerAdvice)
└── util/             QR (ZXing), generacion de PDF (Flying Saucer), numeracion de documentos

src/main/resources/
├── application.yml   Configuracion (perfiles dev/prod)
└── templates/         Plantillas Thymeleaf de factura.html y recibo.html (HTML = PDF)
```

Cada modulo del documento funcional se implemento como su propio servicio y controlador:

1. **Autenticacion** (`/api/v1/auth`) — login, refresh token, gestion de cuentas.
2. **Asociados** (`/api/v1/asociados`) — nucleo del sistema (Modulo 5).
3. **Medidores** (`/api/v1/medidores`) — Modulo 6.
4. **Lecturas y Consumo** (`/api/v1/lecturas`) — Modulo 6.
5. **Periodos Contables** (`/api/v1/periodos`) — Modulo 9.
6. **Configuracion del Sistema** (`/api/v1/configuracion`) — Modulo 10.
7. **Facturacion** (`/api/v1/facturas`) — Modulo 7 (HTML, PDF y QR).
8. **Tesoreria** (`/api/v1/tesoreria`) — pagos, multas, ingresos y gastos (Modulo 8).
9. **Recibos** (`/api/v1/recibos`) — HTML, PDF y QR.
10. **Encuestas y Formularios** (`/api/v1/encuestas`) — Modulo 12.
11. **Notificaciones** (`/api/v1/notificaciones`) — Modulo 13.
12. **Portal Publico** (`/api/v1/publicaciones`) — galeria, destacados, categorias, videos (Modulo 11).
13. **Auditoria** (`/api/v1/auditoria`) — trazabilidad de acciones (solo Administrador).
14. **Estadisticas / Dashboard** (`/api/v1/estadisticas`).
15. **Consultas Publicas** (`/api/v1/consultas`) — estado del servicio sin iniciar sesion.

## Ejecutar en local (modo desarrollo — base de datos H2 en memoria)

Requisitos: JDK 17 y Maven (o usar el wrapper `./mvnw` si lo agrega a su entorno).

```bash
mvn spring-boot:run
```

Por defecto arranca con el perfil `dev`, que usa una base de datos **H2 en memoria**
(no requiere instalar nada mas) y crea automaticamente:

- Un usuario **administrador**: `admin` / `Admin#2026` (cambiar en produccion).
- Configuracion inicial basica (tarifas de ejemplo, metodos de pago).
- El año y mes contable actuales, abiertos.

La consola de H2 queda disponible en `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:mem:acueducto`, usuario `sa`, sin contrasena).

## Ejecutar con PostgreSQL (perfil `prod`)

```bash
docker compose up -d          # levanta PostgreSQL en localhost:5432
export SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run
```

Variables de entorno relevantes (ver `application.yml`):

| Variable | Descripcion | Valor por defecto |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexion a PostgreSQL | `jdbc:postgresql://localhost:5432/acueducto_guaduales` |
| `JWT_SECRET` | Secreto de firma de los JWT | **cambiar siempre en produccion** |
| `JWT_EXPIRATION_MS` / `JWT_REFRESH_EXPIRATION_MS` | Vigencia de los tokens | 24h / 7 dias |
| `STORAGE_PATH` | Carpeta donde se guardan logos/firmas/sellos e imagenes | `./storage` |
| `QR_BASE_URL` | Dominio base usado para construir las URL codificadas en los QR | placeholder, actualizar cuando exista el frontend definitivo |
| `CORS_ORIGINS` | Origenes permitidos para el frontend | `http://localhost:3000,http://localhost:5173` |

> **Nota sobre produccion real:** este proyecto usa `ddl-auto: update` para que el
> esquema se genere automaticamente y el backend sea usable de inmediato. Para un
> despliegue productivo formal se recomienda introducir Flyway con scripts de
> migracion versionados (ya esta la dependencia incluida en el `pom.xml` y la carpeta
> `src/main/resources/db/migration` lista para usarse) en lugar de dejar que Hibernate
> modifique el esquema automaticamente.

## Probar la API con Swagger

Con la aplicacion corriendo, abra:

```
http://localhost:8080/swagger-ui.html
```

Pasos para probar endpoints protegidos:

1. Ejecute `POST /api/v1/auth/login` con `{"username": "admin", "password": "Admin#2026"}`.
2. Copie el valor de `accessToken` de la respuesta.
3. Haga clic en **Authorize** (arriba a la derecha) y pegue el token (sin la palabra `Bearer`).
4. Ya puede probar cualquier endpoint segun el rol del token usado.

El JSON de la especificacion OpenAPI esta disponible en `http://localhost:8080/api-docs`.

## Flujo tipico de uso (de principio a fin)

1. `POST /api/v1/medidores` — registrar un medidor.
2. `POST /api/v1/asociados` — registrar un asociado, indicando el numero de medidor.
3. `POST /api/v1/lecturas` — registrar la lectura del periodo para ese medidor.
4. `POST /api/v1/facturas/generar-mes` — genera automaticamente las facturas de todas
   las lecturas pendientes del mes indicado.
5. `GET /api/v1/facturas/{id}/html` o `/pdf` — ver/descargar la factura generada.
6. `POST /api/v1/tesoreria/pagos` — registrar el pago; esto genera automaticamente el
   recibo, actualiza el estado de la factura y notifica al asociado.
7. `GET /api/v1/recibos/{numeroRecibo}/pdf` — descargar el recibo del pago.
8. `POST /api/v1/periodos/meses/{mesId}/cerrar` — cerrar el periodo una vez todas las
   lecturas tienen factura generada.

## Roles y permisos (resumen)

| Rol | Puede |
|---|---|
| **Publico** (sin sesion) | Ver notificaciones/publicaciones/videos publicos, consultar estado de servicio por documento, consultar factura/recibo por QR, responder formularios publicos |
| **Asociado** | Ver unicamente su propia informacion: facturas, recibos, multas, notificaciones personales, responder formularios |
| **Tesorero** | Gestionar asociados, lecturas (solo lectura), registrar pagos/multas/gastos, generar facturacion, crear notificaciones y encuestas |
| **Administrador** | Acceso total: medidores, configuracion del sistema, cierre de periodos, auditoria, eliminacion definitiva de contenido |

La autorizacion se aplica en dos capas: filtros globales en `SecurityConfig` (rutas
publicas vs. autenticadas) y anotaciones `@PreAuthorize` en cada controlador para el
detalle fino por rol, incluyendo verificacion de que un Asociado solo pueda consultar
su propia informacion (`AsociadoSecurity`).

## Reglas de negocio clave implementadas

- El consumo (m3) siempre se calcula en el servidor: `lectura actual - lectura anterior`, nunca puede ser negativo.
- Un asociado nunca se elimina fisicamente si tiene historial: se archiva (baja logica).
- Cambios de tarifa solo afectan facturas generadas *despues* del cambio.
- Registrar un pago es una operacion atomica: actualiza la factura, crea el movimiento
  de tesoreria, genera el recibo y notifica al asociado, todo en una sola transaccion.
- No se puede registrar un pago mayor al saldo pendiente de la factura.
- Un periodo contable no puede cerrarse si existen lecturas sin factura generada.
- Toda accion relevante queda registrada en el modulo de Auditoria (usuario, fecha, IP).
- Numeracion consecutiva y centralizada para facturas, recibos, entradas y salidas.

## Notas sobre el modulo de codigos QR

Segun la definicion funcional, aun no existe una URL de frontend definitiva. Los QR se
generan apuntando a `${QR_BASE_URL}/factura/{numero}`, `.../recibo/{numero}` y
`.../formulario/{codigo}`. Ajuste `QR_BASE_URL` cuando el frontend este desplegado; no
es necesario regenerar los QR existentes si se usa el mismo esquema de rutas.