# API Acueducto Backend - Documentacion de Endpoints

Generado a partir del codigo fuente (controllers, DTOs y entidades) para facilitar la integracion del frontend. Incluye los 17 modulos del sistema, con metodo, ruta, roles requeridos, cuerpo de peticion (request body), cuerpo de respuesta (response body) y parametros.

## Notas generales


- **Base URL**: `/api/v1`
- **Autenticacion**: JWT Bearer (`Authorization: Bearer <token>`). Se obtiene en `POST /api/v1/auth/login`.
- **Formato de error estandar** (todas las respuestas de error 4xx/5xx):
  ```json
  {
    "timestamp": "2026-07-15T10:30:00",
    "status": 400,
    "error": "Error de validacion",
    "mensaje": "Uno o mas campos son invalidos.",
    "path": "/api/v1/publicaciones",
    "detalles": ["titulo: no debe estar vacio"]
  }
  ```
- **Contenido enriquecido / URLs largas**: los campos de contenido HTML (`contenidoCompleto`), URLs de imagenes (`imagenUrl`), URLs de video (`urlVideo`), enlaces (`enlaceUrl`, `comprobanteUrl`) y rutas de archivos institucionales ahora se guardan como texto sin limite de longitud (columna `TEXT`), asi que aceptan URLs largas, comillas, saltos dentro del HTML de una sola linea, emojis, tildes, etc. Antes de este ajuste estos campos tenian un limite de 300 caracteres en base de datos y una URL larga (por ejemplo un link firmado de un CDN) producia un error 500.
- **Errores 500 por datos invalidos**: se agrego un manejo global para que un dato demasiado largo, un JSON mal formado o un parametro con formato incorrecto respondan con un **400** claro (`Datos invalidos` / `Error de validacion` / `Cuerpo de la peticion invalido`) en vez de un 500 generico. Si el frontend recibe un 500 real de aqui en adelante, es un error interno genuino que vale la pena reportar.
- **Roles**: `ADMINISTRADOR`, `TESORERO`, `ASOCIADO`. Un endpoint sin anotacion de rol pero fuera de las rutas publicas exige solo estar autenticado (cualquier rol).
- **Paginacion**: los endpoints que devuelven `Page<...>` aceptan parametros estandar de Spring: `?page=0&size=20&sort=campo,asc`.
- **Codificacion**: enviar siempre `Content-Type: application/json; charset=UTF-8`.

## Indice de modulos

- [01. Autenticacion](#01-autenticacion)
- [02. Asociados](#02-asociados)
- [03. Medidores](#03-medidores)
- [04. Lecturas y Consumo](#04-lecturas-y-consumo)
- [05. Periodos Contables](#05-periodos-contables)
- [06. Configuracion del Sistema](#06-configuracion-del-sistema)
- [07. Facturacion](#07-facturacion)
- [08. Tesoreria](#08-tesoreria)
- [09. Recibos](#09-recibos)
- [10. Encuestas y Formularios](#10-encuestas-y-formularios)
- [11. Notificaciones](#11-notificaciones)
- [12. Portal Publico](#12-portal-publico)
- [13. Auditoria](#13-auditoria)
- [14. Estadisticas / Dashboard](#14-estadisticas-dashboard)
- [15. Consultas Publicas](#15-consultas-publicas)
- [16. Informes](#16-informes)
- [17. Reportes Ciudadanos](#17-reportes-ciudadanos)


---

## 01. Autenticacion 📌

**Base path:** `/api/v1/auth`

### `POST /api/v1/auth/login`📌
**Iniciar sesion**
_Autentica a un Asociado, Tesorero o Administrador y retorna un JWT (2.3)._
- **Roles:** Publico (sin @PreAuthorize)

**Request body** (`LoginRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `username` | String | **obligatorio**, no vacio |
| `password` | String | **obligatorio**, no vacio |

**Response body** (`LoginResponse`):

| Campo | Tipo |
|---|---|
| `accessToken` | String |
| `refreshToken` | String |
| `tokenType` | String |
| `expiresInMs` | Long |
| `usuario` | UsuarioResponse |

### `POST /api/v1/auth/refresh`📌
**Refrescar token de acceso**
- **Roles:** Publico (sin @PreAuthorize)

**Request body** (`RefreshTokenRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `refreshToken` | String | **obligatorio**, no vacio |

**Response body** (`LoginResponse`):

| Campo | Tipo |
|---|---|
| `accessToken` | String |
| `refreshToken` | String |
| `tokenType` | String |
| `expiresInMs` | Long |
| `usuario` | UsuarioResponse |

### `POST /api/v1/auth/logout`📌
**Cerrar sesion**
_Finaliza la sesion activa (2.3)._
- **Roles:** Publico (sin @PreAuthorize)

**Response body:** sin contenido (204/200 vacio).

### `POST /api/v1/auth/usuarios`
**Crear una cuenta de usuario**
_Solo el Administrador puede crear cuentas de Tesorero o Administrador; el Tesorero puede crear cuentas de Asociado._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')

**Request body** (`CrearUsuarioRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `username` | String | **obligatorio**, no vacio |
| `password` | String | **obligatorio**, no vacio, minimo 8 caracteres |
| `email` | String | **obligatorio**, no vacio, formato email valido |
| `rol` | Rol | **obligatorio** |
| `asociadoId` | Long | - |

**Response body** (`UsuarioResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `username` | String |
| `email` | String |
| `rol` | Rol |
| `activo` | boolean |
| `asociadoId` | Long |

### `GET /api/v1/auth/perfil`
**Perfil del usuario autenticado**
- **Roles:** Publico (sin @PreAuthorize)

**Response body** (`UsuarioResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `username` | String |
| `email` | String |
| `rol` | Rol |
| `activo` | boolean |
| `asociadoId` | Long |

### `PUT /api/v1/auth/cambiar-password`
**Cambiar contrasena propia**
- **Roles:** Publico (sin @PreAuthorize)

**Request body** (`CambiarPasswordRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `passwordActual` | String | **obligatorio**, no vacio |
| `passwordNueva` | String | **obligatorio**, no vacio, minimo 8 caracteres |

**Response body:** sin contenido (204/200 vacio).


---

## 02. Asociados 📌

**Base path:** `/api/v1/asociados`

### `POST /api/v1/asociados`
**Crear asociado**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')

**Request body** (`AsociadoRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `tipoDocumento` | TipoDocumento | **obligatorio** |
| `documento` | String | **obligatorio**, no vacio |
| `nombres` | String | **obligatorio**, no vacio |
| `apellidos` | String | **obligatorio**, no vacio |
| `fechaNacimiento` | LocalDate | - |
| `telefonoPrincipal` | String | **obligatorio**, no vacio |
| `telefonoAlternativo` | String | - |
| `correo` | String | formato email valido |
| `direccion` | String | **obligatorio**, no vacio |
| `barrioVereda` | String | - |
| `observaciones` | String | - |
| `numeroMedidor` | String | **obligatorio**, no vacio |
| `fechaAfiliacion` | LocalDate | - |

**Response body** (`AsociadoResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigoInterno` | String |
| `tipoDocumento` | TipoDocumento |
| `documento` | String |
| `nombres` | String |
| `apellidos` | String |
| `telefonoPrincipal` | String |
| `telefonoAlternativo` | String |
| `correo` | String |
| `direccion` | String |
| `barrioVereda` | String |
| `estadoServicio` | EstadoServicio |
| `fechaAfiliacion` | LocalDate |
| `numeroMedidor` | String |
| `archivado` | boolean |

### `PUT /api/v1/asociados/{id}`
**Editar asociado**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `id` (Long)

**Request body** (`AsociadoRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `tipoDocumento` | TipoDocumento | **obligatorio** |
| `documento` | String | **obligatorio**, no vacio |
| `nombres` | String | **obligatorio**, no vacio |
| `apellidos` | String | **obligatorio**, no vacio |
| `fechaNacimiento` | LocalDate | - |
| `telefonoPrincipal` | String | **obligatorio**, no vacio |
| `telefonoAlternativo` | String | - |
| `correo` | String | formato email valido |
| `direccion` | String | **obligatorio**, no vacio |
| `barrioVereda` | String | - |
| `observaciones` | String | - |
| `numeroMedidor` | String | **obligatorio**, no vacio |
| `fechaAfiliacion` | LocalDate | - |

**Response body** (`AsociadoResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigoInterno` | String |
| `tipoDocumento` | TipoDocumento |
| `documento` | String |
| `nombres` | String |
| `apellidos` | String |
| `telefonoPrincipal` | String |
| `telefonoAlternativo` | String |
| `correo` | String |
| `direccion` | String |
| `barrioVereda` | String |
| `estadoServicio` | EstadoServicio |
| `fechaAfiliacion` | LocalDate |
| `numeroMedidor` | String |
| `archivado` | boolean |

### `GET /api/v1/asociados`
**Buscar/listar asociados**
_Busca por documento, nombre, apellidos o telefono (5.10)._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Query params:** `q` (String, opcional)

**Response body** (Lista de `AsociadoResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigoInterno` | String |
| `tipoDocumento` | TipoDocumento |
| `documento` | String |
| `nombres` | String |
| `apellidos` | String |
| `telefonoPrincipal` | String |
| `telefonoAlternativo` | String |
| `correo` | String |
| `direccion` | String |
| `barrioVereda` | String |
| `estadoServicio` | EstadoServicio |
| `fechaAfiliacion` | LocalDate |
| `numeroMedidor` | String |
| `archivado` | boolean |

### `GET /api/v1/asociados/filtrar`
**Filtrar asociados por estado de servicio**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Query params:** `estado` (EstadoServicio, requerido)

**Response body** (Lista de `AsociadoResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigoInterno` | String |
| `tipoDocumento` | TipoDocumento |
| `documento` | String |
| `nombres` | String |
| `apellidos` | String |
| `telefonoPrincipal` | String |
| `telefonoAlternativo` | String |
| `correo` | String |
| `direccion` | String |
| `barrioVereda` | String |
| `estadoServicio` | EstadoServicio |
| `fechaAfiliacion` | LocalDate |
| `numeroMedidor` | String |
| `archivado` | boolean |

### `GET /api/v1/asociados/{id}`
**Ver detalle de un asociado**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO') or (hasRole('ASOCIADO') and @asociadoSecurity.esPropio(#id))
- **Path params:** `id` (Long)

**Response body** (`AsociadoResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigoInterno` | String |
| `tipoDocumento` | TipoDocumento |
| `documento` | String |
| `nombres` | String |
| `apellidos` | String |
| `telefonoPrincipal` | String |
| `telefonoAlternativo` | String |
| `correo` | String |
| `direccion` | String |
| `barrioVereda` | String |
| `estadoServicio` | EstadoServicio |
| `fechaAfiliacion` | LocalDate |
| `numeroMedidor` | String |
| `archivado` | boolean |

### `GET /api/v1/asociados/{id}/resumen-financiero`
**Resumen financiero del asociado**
_Calculado dinamicamente: facturas, pagos y multas (5.4)._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO') or (hasRole('ASOCIADO') and @asociadoSecurity.esPropio(#id))
- **Path params:** `id` (Long)

**Response body** (`AsociadoResumenFinancieroResponse`):

| Campo | Tipo |
|---|---|
| `totalFacturas` | long |
| `facturasPagadas` | long |
| `facturasPendientes` | long |
| `totalPagado` | BigDecimal |
| `totalPendiente` | BigDecimal |
| `numeroMultas` | long |
| `totalMultas` | BigDecimal |

### `PATCH /api/v1/asociados/{id}/estado-servicio`
**Cambiar estado del servicio**
_Activo o Suspendido (5.7). No afecta el acceso a la plataforma._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `id` (Long)

**Request body** (`CambioEstadoServicioRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `estado` | EstadoServicio | **obligatorio** |
| `motivo` | String | - |

**Response body** (`AsociadoResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigoInterno` | String |
| `tipoDocumento` | TipoDocumento |
| `documento` | String |
| `nombres` | String |
| `apellidos` | String |
| `telefonoPrincipal` | String |
| `telefonoAlternativo` | String |
| `correo` | String |
| `direccion` | String |
| `barrioVereda` | String |
| `estadoServicio` | EstadoServicio |
| `fechaAfiliacion` | LocalDate |
| `numeroMedidor` | String |
| `archivado` | boolean |

### `DELETE /api/v1/asociados/{id}`
**Archivar asociado (baja logica)**
_Nunca se elimina fisicamente si tiene historial (5.8)._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `id` (Long)

**Response body:** sin contenido (204/200 vacio).


---

## 03. Medidores 📌

**Base path:** `/api/v1/medidores`

### `POST /api/v1/medidores`
**Registrar medidor**
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)

**Request body** (`MedidorRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `numero` | String | **obligatorio**, no vacio |
| `asociadoId` | Long | - |
| `fechaInstalacion` | LocalDate | - |
| `ubicacion` | String | - |
| `observaciones` | String | - |

**Response body** (`MedidorResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigoInterno` | String |
| `numero` | String |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `fechaInstalacion` | LocalDate |
| `estado` | EstadoMedidor |
| `ubicacion` | String |

### `PUT /api/v1/medidores/{id}`
**Editar medidor**
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)
- **Path params:** `id` (Long)

**Request body** (`MedidorRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `numero` | String | **obligatorio**, no vacio |
| `asociadoId` | Long | - |
| `fechaInstalacion` | LocalDate | - |
| `ubicacion` | String | - |
| `observaciones` | String | - |

**Response body** (`MedidorResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigoInterno` | String |
| `numero` | String |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `fechaInstalacion` | LocalDate |
| `estado` | EstadoMedidor |
| `ubicacion` | String |

### `PATCH /api/v1/medidores/{id}/estado`
**Cambiar estado del medidor**
_Activo, En mantenimiento, Danado o Retirado (6.4)._
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)
- **Path params:** `id` (Long)
- **Query params:** `estado` (EstadoMedidor, requerido)

**Response body** (`MedidorResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigoInterno` | String |
| `numero` | String |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `fechaInstalacion` | LocalDate |
| `estado` | EstadoMedidor |
| `ubicacion` | String |

### `GET /api/v1/medidores`
**Listar medidores**
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)

**Response body** (Lista de `MedidorResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigoInterno` | String |
| `numero` | String |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `fechaInstalacion` | LocalDate |
| `estado` | EstadoMedidor |
| `ubicacion` | String |

### `GET /api/v1/medidores/{id}`
**Ver detalle de un medidor**
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)
- **Path params:** `id` (Long)

**Response body** (`MedidorResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigoInterno` | String |
| `numero` | String |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `fechaInstalacion` | LocalDate |
| `estado` | EstadoMedidor |
| `ubicacion` | String |


---

## 04. Lecturas y Consumo  

**Base path:** `/api/v1/lecturas`

### `POST /api/v1/lecturas`
**Registrar lectura**
_Calcula el consumo automaticamente (actual - anterior). Nunca puede ser negativo (6.6)._
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)

**Request body** (`LecturaRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `medidorId` | Long | **obligatorio** |
| `mesContableId` | Long | **obligatorio** |
| `fechaLectura` | LocalDate | **obligatorio** |
| `lecturaActual` | Integer | **obligatorio**, debe ser >= 0 |
| `observaciones` | String | - |

**Response body** (`LecturaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `medidorId` | Long |
| `numeroMedidor` | String |
| `mesContableId` | Long |
| `fechaLectura` | LocalDate |
| `lecturaAnterior` | Integer |
| `lecturaActual` | Integer |
| `consumoM3` | Integer |
| `facturaGenerada` | boolean |

### `PUT /api/v1/lecturas/{id}`
**Editar lectura**
_Solo si aun no ha generado factura (6.14)._
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)
- **Path params:** `id` (Long)

**Request body** (`LecturaRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `medidorId` | Long | **obligatorio** |
| `mesContableId` | Long | **obligatorio** |
| `fechaLectura` | LocalDate | **obligatorio** |
| `lecturaActual` | Integer | **obligatorio**, debe ser >= 0 |
| `observaciones` | String | - |

**Response body** (`LecturaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `medidorId` | Long |
| `numeroMedidor` | String |
| `mesContableId` | Long |
| `fechaLectura` | LocalDate |
| `lecturaAnterior` | Integer |
| `lecturaActual` | Integer |
| `consumoM3` | Integer |
| `facturaGenerada` | boolean |

### `GET /api/v1/lecturas/{id}`
**Ver detalle de una lectura**
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)
- **Path params:** `id` (Long)

**Response body** (`LecturaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `medidorId` | Long |
| `numeroMedidor` | String |
| `mesContableId` | Long |
| `fechaLectura` | LocalDate |
| `lecturaAnterior` | Integer |
| `lecturaActual` | Integer |
| `consumoM3` | Integer |
| `facturaGenerada` | boolean |

### `GET /api/v1/lecturas/asociado/{asociadoId}`
**Historial de consumo de un asociado**
_Todas las lecturas registradas (6.10)._
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)
- **Path params:** `asociadoId` (Long)

**Response body** (Lista de `LecturaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `medidorId` | Long |
| `numeroMedidor` | String |
| `mesContableId` | Long |
| `fechaLectura` | LocalDate |
| `lecturaAnterior` | Integer |
| `lecturaActual` | Integer |
| `consumoM3` | Integer |
| `facturaGenerada` | boolean |

### `GET /api/v1/lecturas/mes/{mesContableId}`
**Lecturas registradas en un periodo**
_Usado antes de ejecutar la Generacion de Facturacion del Mes (6.14)._
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)
- **Path params:** `mesContableId` (Long)

**Response body** (Lista de `LecturaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `medidorId` | Long |
| `numeroMedidor` | String |
| `mesContableId` | Long |
| `fechaLectura` | LocalDate |
| `lecturaAnterior` | Integer |
| `lecturaActual` | Integer |
| `consumoM3` | Integer |
| `facturaGenerada` | boolean |


---

## 05. Periodos Contables

**Base path:** `/api/v1/periodos`

### `POST /api/v1/periodos/anios`
**Crear anio contable**
- **Roles:** hasRole('ADMINISTRADOR')

**Request body** (`AnioContableRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `anio` | Integer | **obligatorio** |

**Response body** (`AnioContableResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `anio` | Integer |
| `estado` | EstadoAnio |

### `GET /api/v1/periodos/anios`
**Listar anios contables**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')

**Response body** (Lista de `AnioContableResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `anio` | Integer |
| `estado` | EstadoAnio |

### `POST /api/v1/periodos/meses`
**Crear mes contable**
- **Roles:** hasRole('ADMINISTRADOR')

**Request body** (`MesContableRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `anioContableId` | Long | **obligatorio** |
| `numeroMes` | Integer | **obligatorio**, min 1, max 12 |

**Response body** (`MesContableResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombreMes` | String |
| `numeroMes` | Integer |
| `anio` | Integer |
| `estado` | EstadoMes |
| `fechaApertura` | LocalDate |
| `fechaCierre` | LocalDate |

### `GET /api/v1/periodos/anios/{anioId}/meses`
**Listar meses de un anio**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `anioId` (Long)

**Response body** (Lista de `MesContableResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombreMes` | String |
| `numeroMes` | Integer |
| `anio` | Integer |
| `estado` | EstadoMes |
| `fechaApertura` | LocalDate |
| `fechaCierre` | LocalDate |

### `POST /api/v1/periodos/meses/{mesId}/cerrar`
**Cerrar periodo**
_Verifica que todas las lecturas y facturas esten completas antes de cerrar (9.8)._
- **Roles:** hasRole('ADMINISTRADOR')
- **Path params:** `mesId` (Long)

**Response body** (`MesContableResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombreMes` | String |
| `numeroMes` | Integer |
| `anio` | Integer |
| `estado` | EstadoMes |
| `fechaApertura` | LocalDate |
| `fechaCierre` | LocalDate |

### `POST /api/v1/periodos/meses/{mesId}/reabrir`
**Reabrir periodo**
_Uso excepcional para correcciones administrativas; queda registrado en auditoria (9.6)._
- **Roles:** hasRole('ADMINISTRADOR')
- **Path params:** `mesId` (Long)
- **Query params:** `motivo` (String, opcional)

**Response body** (`MesContableResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombreMes` | String |
| `numeroMes` | Integer |
| `anio` | Integer |
| `estado` | EstadoMes |
| `fechaApertura` | LocalDate |
| `fechaCierre` | LocalDate |

### `GET /api/v1/periodos/meses/{mesId}/resumen`
**Resumen del periodo**
_Indicadores de facturacion, tesoreria, consumo y asociados (9.9)._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `mesId` (Long)

**Response body** (`ResumenPeriodoResponse`):

| Campo | Tipo |
|---|---|
| `facturasGeneradas` | long |
| `facturasPagadas` | long |
| `facturasPendientes` | long |
| `facturasVencidas` | long |
| `totalIngresos` | BigDecimal |
| `totalGastos` | BigDecimal |
| `balance` | BigDecimal |
| `totalM3Consumidos` | long |
| `promedioConsumo` | double |
| `asociadosActivos` | long |
| `asociadosSuspendidos` | long |


---

## 06. Configuracion del Sistema

**Base path:** `/api/v1/configuracion`

### `GET /api/v1/configuracion`
**Consultar configuracion actual**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')

**Response body** (`ConfiguracionResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombreAcueducto` | String |
| `nit` | String |
| `direccion` | String |
| `telefonoPrincipal` | String |
| `correo` | String |
| `municipio` | String |
| `departamento` | String |
| `banco` | String |
| `tipoCuenta` | String |
| `numeroCuenta` | String |
| `titularCuenta` | String |
| `valorM3` | BigDecimal |
| `cargoFijoAdministracion` | BigDecimal |
| `valorReconexion` | BigDecimal |
| `valorMultaDefecto` | BigDecimal |
| `diasPlazoPago` | Integer |
| `notasFactura` | String |
| `logoActivo` | String |
| `firmaActiva` | String |
| `selloActivo` | String |

### `PUT /api/v1/configuracion`
**Actualizar configuracion**
_Los cambios de tarifa solo afectan las facturas nuevas (6.7 / 10.6)._
- **Roles:** hasRole('ADMINISTRADOR')

**Request body** (`ConfiguracionRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `nombreAcueducto` | String | **obligatorio**, no vacio |
| `nit` | String | - |
| `direccion` | String | - |
| `telefonoPrincipal` | String | - |
| `correo` | String | - |
| `municipio` | String | - |
| `departamento` | String | - |
| `banco` | String | - |
| `tipoCuenta` | String | - |
| `numeroCuenta` | String | - |
| `titularCuenta` | String | - |
| `valorM3` | BigDecimal | **obligatorio**, debe ser > 0 |
| `cargoFijoAdministracion` | BigDecimal | **obligatorio**, debe ser > 0 |
| `valorReconexion` | BigDecimal | - |
| `valorMultaDefecto` | BigDecimal | - |
| `diasPlazoPago` | Integer | - |
| `notasFactura` | String | - |

**Response body** (`ConfiguracionResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombreAcueducto` | String |
| `nit` | String |
| `direccion` | String |
| `telefonoPrincipal` | String |
| `correo` | String |
| `municipio` | String |
| `departamento` | String |
| `banco` | String |
| `tipoCuenta` | String |
| `numeroCuenta` | String |
| `titularCuenta` | String |
| `valorM3` | BigDecimal |
| `cargoFijoAdministracion` | BigDecimal |
| `valorReconexion` | BigDecimal |
| `valorMultaDefecto` | BigDecimal |
| `diasPlazoPago` | Integer |
| `notasFactura` | String |
| `logoActivo` | String |
| `firmaActiva` | String |
| `selloActivo` | String |

### `POST /api/v1/configuracion/metodos-pago`
**Crear metodo de pago**
- **Roles:** hasRole('ADMINISTRADOR')

**Request body** (`MetodoPagoRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `nombre` | String | **obligatorio**, no vacio |

**Response body** (`MetodoPago`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombre` | String (max 60) |
| `activo` | boolean |

### `PATCH /api/v1/configuracion/metodos-pago/{id}`
**Activar/desactivar metodo de pago**
- **Roles:** hasRole('ADMINISTRADOR')
- **Path params:** `id` (Long)
- **Query params:** `activo` (boolean, requerido)

**Response body** (`MetodoPago`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombre` | String (max 60) |
| `activo` | boolean |

### `GET /api/v1/configuracion/metodos-pago`
**Listar metodos de pago disponibles (activos)**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')

**Response body** (Lista de `MetodoPago`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombre` | String (max 60) |
| `activo` | boolean |

### `POST /api/v1/configuracion/archivos/{tipo}`
**Subir archivo institucional**
_Logo, firma o sello. Se almacena en /storage/config (7.12 / 10.9)._
- **Roles:** hasRole('ADMINISTRADOR')
- **Path params:** `tipo` (TipoArchivoInstitucional)
- **Query params:** `archivo` (MultipartFile, requerido)

**Response body** (`ArchivoInstitucional`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `tipo` | TipoArchivoInstitucional (LOGO/FIRMA/SELLO) |
| `nombreArchivo` | String (max 200) |
| `ruta` | String (sin limite - TEXT) |
| `fuente` | FuenteArchivoInstitucional (STORAGE/URL) |
| `activo` | boolean |

### `POST /api/v1/configuracion/archivos/{tipo}/url`
**Registrar archivo institucional desde URL**
_Permite usar un logo, firma o sello remoto sin subirlo al storage del servidor. Acepta JSON con la URL en el body para evitar problemas con caracteres especiales o URLs largas._
- **Roles:** hasRole('ADMINISTRADOR')
- **Path params:** `tipo` (TipoArchivoInstitucional)

**Request body** (`ArchivoInstitucionalUrlRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `url` | String | **obligatorio**, no vacio |
| `nombreArchivo` | String | - |

**Response body** (`ArchivoInstitucional`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `tipo` | TipoArchivoInstitucional (LOGO/FIRMA/SELLO) |
| `nombreArchivo` | String (max 200) |
| `ruta` | String (sin limite - TEXT) |
| `fuente` | FuenteArchivoInstitucional (STORAGE/URL) |
| `activo` | boolean |

### `PATCH /api/v1/configuracion/archivos/{archivoId}/activar`
**Activar archivo institucional**
_Selecciona cual logo/firma/sello se usara en los documentos (10.10)._
- **Roles:** hasRole('ADMINISTRADOR')
- **Path params:** `archivoId` (Long)

**Response body:** sin contenido (204/200 vacio).

### `GET /api/v1/configuracion/archivos/{tipo}`
**Listar archivos institucionales por tipo**
- **Roles:** hasRole('ADMINISTRADOR')
- **Path params:** `tipo` (TipoArchivoInstitucional)

**Response body** (Lista de `ArchivoInstitucional`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `tipo` | TipoArchivoInstitucional (LOGO/FIRMA/SELLO) |
| `nombreArchivo` | String (max 200) |
| `ruta` | String (sin limite - TEXT) |
| `fuente` | FuenteArchivoInstitucional (STORAGE/URL) |
| `activo` | boolean |

### `POST /api/v1/configuracion/archivos/{tipo}/sincronizar`
**Sincronizar archivos institucionales**
_Detecta imagenes ya colocadas directamente en storage/config/{logo|firma|sello} en el servidor _
- **Roles:** hasRole('ADMINISTRADOR')
- **Path params:** `tipo` (TipoArchivoInstitucional)

**Response body** (Lista de `ArchivoInstitucional`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `tipo` | TipoArchivoInstitucional (LOGO/FIRMA/SELLO) |
| `nombreArchivo` | String (max 200) |
| `ruta` | String (sin limite - TEXT) |
| `fuente` | FuenteArchivoInstitucional (STORAGE/URL) |
| `activo` | boolean |


---

## 07. Facturacion

**Base path:** `/api/v1/facturas`

### `POST /api/v1/facturas/generar-mes`
**Generar facturacion del mes**
_Procesa todas las lecturas pendientes del periodo y genera una factura por cada una (6.14)._
- **Roles:** hasRole('ADMINISTRADOR')

**Request body** (`GenerarFacturacionMesRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `mesContableId` | Long | **obligatorio** |

**Response body** (Lista de `FacturaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `numeroFactura` | String |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `numeroMedidor` | String |
| `fechaEmision` | LocalDate |
| `fechaLimitePago` | LocalDate |
| `lecturaAnterior` | Integer |
| `lecturaActual` | Integer |
| `consumoM3` | Integer |
| `valorConsumo` | BigDecimal |
| `cargoAdministracion` | BigDecimal |
| `valoresAdicionales` | BigDecimal |
| `totalMultas` | BigDecimal |
| `total` | BigDecimal |
| `totalPagado` | BigDecimal |
| `saldoPendiente` | BigDecimal |
| `estado` | EstadoFactura |
| `codigoQr` | String |

### `POST /api/v1/facturas/conceptos`
**Agregar concepto adicional a una factura**
- **Roles:** hasRole('ADMINISTRADOR')

**Request body** (`ConceptoFacturaRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `facturaId` | Long | **obligatorio** |
| `descripcion` | String | **obligatorio**, no vacio |
| `valor` | BigDecimal | **obligatorio**, debe ser > 0 |

**Response body** (`FacturaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `numeroFactura` | String |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `numeroMedidor` | String |
| `fechaEmision` | LocalDate |
| `fechaLimitePago` | LocalDate |
| `lecturaAnterior` | Integer |
| `lecturaActual` | Integer |
| `consumoM3` | Integer |
| `valorConsumo` | BigDecimal |
| `cargoAdministracion` | BigDecimal |
| `valoresAdicionales` | BigDecimal |
| `totalMultas` | BigDecimal |
| `total` | BigDecimal |
| `totalPagado` | BigDecimal |
| `saldoPendiente` | BigDecimal |
| `estado` | EstadoFactura |
| `codigoQr` | String |

### `POST /api/v1/facturas/{id}/anular`
**Anular factura**
- **Roles:** hasRole('ADMINISTRADOR')
- **Path params:** `id` (Long)
- **Query params:** `motivo` (String, requerido)

**Response body** (`FacturaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `numeroFactura` | String |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `numeroMedidor` | String |
| `fechaEmision` | LocalDate |
| `fechaLimitePago` | LocalDate |
| `lecturaAnterior` | Integer |
| `lecturaActual` | Integer |
| `consumoM3` | Integer |
| `valorConsumo` | BigDecimal |
| `cargoAdministracion` | BigDecimal |
| `valoresAdicionales` | BigDecimal |
| `totalMultas` | BigDecimal |
| `total` | BigDecimal |
| `totalPagado` | BigDecimal |
| `saldoPendiente` | BigDecimal |
| `estado` | EstadoFactura |
| `codigoQr` | String |

### `GET /api/v1/facturas/{id}`
**Ver detalle de una factura**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO') or hasRole('ASOCIADO')
- **Path params:** `id` (Long)

**Response body** (`FacturaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `numeroFactura` | String |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `numeroMedidor` | String |
| `fechaEmision` | LocalDate |
| `fechaLimitePago` | LocalDate |
| `lecturaAnterior` | Integer |
| `lecturaActual` | Integer |
| `consumoM3` | Integer |
| `valorConsumo` | BigDecimal |
| `cargoAdministracion` | BigDecimal |
| `valoresAdicionales` | BigDecimal |
| `totalMultas` | BigDecimal |
| `total` | BigDecimal |
| `totalPagado` | BigDecimal |
| `saldoPendiente` | BigDecimal |
| `estado` | EstadoFactura |
| `codigoQr` | String |

### `GET /api/v1/facturas/asociado/{asociadoId}`
**Listar facturas de un asociado**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO') or (hasRole('ASOCIADO') and @asociadoSecurity.esPropio(#asociadoId))
- **Path params:** `asociadoId` (Long)

**Response body** (Pagina de `FacturaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `numeroFactura` | String |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `numeroMedidor` | String |
| `fechaEmision` | LocalDate |
| `fechaLimitePago` | LocalDate |
| `lecturaAnterior` | Integer |
| `lecturaActual` | Integer |
| `consumoM3` | Integer |
| `valorConsumo` | BigDecimal |
| `cargoAdministracion` | BigDecimal |
| `valoresAdicionales` | BigDecimal |
| `totalMultas` | BigDecimal |
| `total` | BigDecimal |
| `totalPagado` | BigDecimal |
| `saldoPendiente` | BigDecimal |
| `estado` | EstadoFactura |
| `codigoQr` | String |

### `GET /api/v1/facturas`
**Listar facturas por estado**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Query params:** `estado` (EstadoFactura, requerido)

**Response body** (Pagina de `FacturaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `numeroFactura` | String |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `numeroMedidor` | String |
| `fechaEmision` | LocalDate |
| `fechaLimitePago` | LocalDate |
| `lecturaAnterior` | Integer |
| `lecturaActual` | Integer |
| `consumoM3` | Integer |
| `valorConsumo` | BigDecimal |
| `cargoAdministracion` | BigDecimal |
| `valoresAdicionales` | BigDecimal |
| `totalMultas` | BigDecimal |
| `total` | BigDecimal |
| `totalPagado` | BigDecimal |
| `saldoPendiente` | BigDecimal |
| `estado` | EstadoFactura |
| `codigoQr` | String |

### `GET /api/v1/facturas/todas`
**Historial completo de facturas**
_Todas las facturas del sistema, sin filtrar por estado ni asociado. Admite orden y paginacion (ej: ?sort=fechaEmision,desc)._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')

**Response body** (Pagina de `FacturaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `numeroFactura` | String |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `numeroMedidor` | String |
| `fechaEmision` | LocalDate |
| `fechaLimitePago` | LocalDate |
| `lecturaAnterior` | Integer |
| `lecturaActual` | Integer |
| `consumoM3` | Integer |
| `valorConsumo` | BigDecimal |
| `cargoAdministracion` | BigDecimal |
| `valoresAdicionales` | BigDecimal |
| `totalMultas` | BigDecimal |
| `total` | BigDecimal |
| `totalPagado` | BigDecimal |
| `saldoPendiente` | BigDecimal |
| `estado` | EstadoFactura |
| `codigoQr` | String |

### `GET /api/v1/facturas/{id}/html`
**Ver factura en HTML**
_Version oficial en linea, identica a la version PDF (7.9)._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO') or hasRole('ASOCIADO')
- **Path params:** `id` (Long)

**Response body:** texto plano/HTML (`text/html`).

### `GET /api/v1/facturas/{id}/pdf`
**Descargar factura en PDF**
_Mismo diseno que la version HTML (7.10)._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO') or hasRole('ASOCIADO')
- **Path params:** `id` (Long)

**Response body:** archivo binario (PDF, `application/pdf`).

### `GET /api/v1/facturas/qr/{numeroFactura}`
**Consultar factura via codigo QR**
_Endpoint publico usado al escanear el QR (7.11). El Tesorero puede continuar hacia el registro del pago; el Asociado solo consulta._
- **Roles:** Publico (sin @PreAuthorize)
- **Path params:** `numeroFactura` (String)

**Response body** (`FacturaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `numeroFactura` | String |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `numeroMedidor` | String |
| `fechaEmision` | LocalDate |
| `fechaLimitePago` | LocalDate |
| `lecturaAnterior` | Integer |
| `lecturaActual` | Integer |
| `consumoM3` | Integer |
| `valorConsumo` | BigDecimal |
| `cargoAdministracion` | BigDecimal |
| `valoresAdicionales` | BigDecimal |
| `totalMultas` | BigDecimal |
| `total` | BigDecimal |
| `totalPagado` | BigDecimal |
| `saldoPendiente` | BigDecimal |
| `estado` | EstadoFactura |
| `codigoQr` | String |


---

## 08. Tesoreria

**Base path:** `/api/v1/tesoreria`

### `POST /api/v1/tesoreria/pagos`
**Registrar pago de factura**
_Operacion atomica: actualiza la factura, crea el movimiento, genera el recibo y notifica al asociado (8.5)._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)

**Request body** (`RegistrarPagoRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `facturaId` | Long | **obligatorio** |
| `valor` | BigDecimal | **obligatorio**, debe ser > 0 |
| `metodoPagoId` | Long | **obligatorio** |
| `observaciones` | String | - |

**Response body** (`PagoResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `numeroFactura` | String |
| `asociadoId` | Long |
| `valor` | BigDecimal |
| `fecha` | LocalDateTime |
| `metodoPago` | String |
| `tesorero` | String |
| `numeroRecibo` | String |

### `POST /api/v1/tesoreria/multas`
**Registrar multa**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)

**Request body** (`MultaRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `asociadoId` | Long | **obligatorio** |
| `facturaId` | Long | - |
| `motivo` | String | **obligatorio**, no vacio, maximo 200 caracteres |
| `valor` | BigDecimal | **obligatorio**, debe ser > 0 |

**Response body** (`MultaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `asociadoId` | Long |
| `motivo` | String |
| `valor` | BigDecimal |
| `fecha` | LocalDate |
| `estado` | EstadoMulta |

### `GET /api/v1/tesoreria/multas/asociado/{asociadoId}`
**Listar multas de un asociado**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)
- **Path params:** `asociadoId` (Long)

**Response body** (Lista de `MultaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `asociadoId` | Long |
| `motivo` | String |
| `valor` | BigDecimal |
| `fecha` | LocalDate |
| `estado` | EstadoMulta |

### `POST /api/v1/tesoreria/ingresos`
**Registrar ingreso extraordinario**
_Donaciones, reconexiones, nuevas afiliaciones, otros ingresos (8.4)._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)

**Request body** (`MovimientoTesoreriaRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `valor` | BigDecimal | **obligatorio**, debe ser > 0 |
| `metodoPagoId` | Long | **obligatorio** |
| `concepto` | String | **obligatorio**, no vacio, maximo 200 caracteres |
| `categoria` | String | maximo 60 caracteres |
| `observaciones` | String | - |
| `asociadoId` | Long | - |
| `mesContableId` | Long | **obligatorio** |
| `comprobanteUrl` | String | - |

**Response body** (`MovimientoTesoreriaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `tipo` | TipoMovimiento |
| `numeroFormateado` | String |
| `fecha` | LocalDateTime |
| `valor` | BigDecimal |
| `concepto` | String |
| `categoria` | String |
| `usuario` | String |
| `facturaNumero` | String |
| `reciboNumero` | String |

### `POST /api/v1/tesoreria/gastos`
**Registrar gasto**
_Servicios, materiales, reparaciones, personal, otros egresos (8.9)._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)

**Request body** (`MovimientoTesoreriaRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `valor` | BigDecimal | **obligatorio**, debe ser > 0 |
| `metodoPagoId` | Long | **obligatorio** |
| `concepto` | String | **obligatorio**, no vacio, maximo 200 caracteres |
| `categoria` | String | maximo 60 caracteres |
| `observaciones` | String | - |
| `asociadoId` | Long | - |
| `mesContableId` | Long | **obligatorio** |
| `comprobanteUrl` | String | - |

**Response body** (`MovimientoTesoreriaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `tipo` | TipoMovimiento |
| `numeroFormateado` | String |
| `fecha` | LocalDateTime |
| `valor` | BigDecimal |
| `concepto` | String |
| `categoria` | String |
| `usuario` | String |
| `facturaNumero` | String |
| `reciboNumero` | String |

### `POST /api/v1/tesoreria/movimientos/{id}/anular`
**Anular movimiento**
_Exclusivo del Administrador (8.3)._
- **Roles:** hasRole('ADMINISTRADOR')
- **Path params:** `id` (Long)
- **Query params:** `motivo` (String, requerido)

**Response body:** sin contenido (204/200 vacio).

### `GET /api/v1/tesoreria/movimientos`
**Listar movimientos por tipo (entradas o salidas)**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)
- **Query params:** `tipo` (TipoMovimiento, requerido)

**Response body** (Pagina de `MovimientoTesoreriaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `tipo` | TipoMovimiento |
| `numeroFormateado` | String |
| `fecha` | LocalDateTime |
| `valor` | BigDecimal |
| `concepto` | String |
| `categoria` | String |
| `usuario` | String |
| `facturaNumero` | String |
| `reciboNumero` | String |

### `GET /api/v1/tesoreria/movimientos/todos`
**Historial combinado de movimientos**
_Entradas y salidas juntas en una sola lista, ordenadas (ej: ?sort=fecha,desc)._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)

**Response body** (Pagina de `MovimientoTesoreriaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `tipo` | TipoMovimiento |
| `numeroFormateado` | String |
| `fecha` | LocalDateTime |
| `valor` | BigDecimal |
| `concepto` | String |
| `categoria` | String |
| `usuario` | String |
| `facturaNumero` | String |
| `reciboNumero` | String |

### `GET /api/v1/tesoreria/caja-diaria`
**Caja diaria**
_Ingresos, gastos y balance del dia actual (8.10)._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)

**Response body** (`CajaDiariaResponse`):

| Campo | Tipo |
|---|---|
| `totalIngresos` | BigDecimal |
| `totalGastos` | BigDecimal |
| `balance` | BigDecimal |
| `numeroMovimientos` | long |


---

## 09. Recibos

**Base path:** `/api/v1/recibos`

### `GET /api/v1/recibos/asociado/{asociadoId}`
**Listar recibos de un asociado**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO') or (hasRole('ASOCIADO') and @asociadoSecurity.esPropio(#asociadoId))
- **Path params:** `asociadoId` (Long)

**Response body** (Pagina de `ReciboResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `numeroRecibo` | String |
| `numeroFactura` | String |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `fechaEmision` | LocalDateTime |
| `valor` | BigDecimal |
| `saldoPendiente` | BigDecimal |
| `metodoPago` | String |
| `estado` | EstadoRecibo |
| `codigoQr` | String |

### `GET /api/v1/recibos/{numeroRecibo}/html`
**Ver recibo en HTML**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO') or hasRole('ASOCIADO')
- **Path params:** `numeroRecibo` (String)

**Response body:** texto plano/HTML (`text/html`).

### `GET /api/v1/recibos/{numeroRecibo}/pdf`
**Descargar recibo en PDF**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO') or hasRole('ASOCIADO')
- **Path params:** `numeroRecibo` (String)

**Response body:** archivo binario (PDF, `application/pdf`).

### `GET /api/v1/recibos/qr/{numeroRecibo}`
**Consultar recibo via codigo QR**
- **Roles:** Publico (sin @PreAuthorize)
- **Path params:** `numeroRecibo` (String)

**Response body** (`ReciboResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `numeroRecibo` | String |
| `numeroFactura` | String |
| `asociadoId` | Long |
| `asociadoNombre` | String |
| `fechaEmision` | LocalDateTime |
| `valor` | BigDecimal |
| `saldoPendiente` | BigDecimal |
| `metodoPago` | String |
| `estado` | EstadoRecibo |
| `codigoQr` | String |


---

## 10. Encuestas y Formularios

**Base path:** `/api/v1/encuestas`

### `POST /api/v1/encuestas`
**Crear formulario**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')

**Request body** (`EncuestaRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `titulo` | String | **obligatorio**, no vacio |
| `descripcion` | String | - |
| `publico` | boolean | - |
| `requiereAutenticacion` | boolean | - |
| `respuestaUnica` | boolean | - |
| `respuestasAnonimas` | boolean | - |
| `fechaInicio` | LocalDateTime | - |
| `fechaFin` | LocalDateTime | - |
| `preguntas` | List<PreguntaEncuestaRequest> | - |

**Response body** (`EncuestaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigo` | String |
| `titulo` | String |
| `descripcion` | String |
| `estado` | EstadoEncuesta |
| `publico` | boolean |
| `requiereAutenticacion` | boolean |
| `respuestaUnica` | boolean |
| `respuestasAnonimas` | boolean |
| `fechaInicio` | LocalDateTime |
| `fechaFin` | LocalDateTime |
| `codigoQr` | String |
| `preguntas` | List<PreguntaResponse> |
| `id` | Long |
| `texto` | String |
| `tipo` | TipoPregunta |
| `obligatoria` | boolean |
| `orden` | Integer |
| `opciones` | List<String> |

### `POST /api/v1/encuestas/{id}/activar`
**Activar formulario**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `id` (Long)

**Response body** (`EncuestaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigo` | String |
| `titulo` | String |
| `descripcion` | String |
| `estado` | EstadoEncuesta |
| `publico` | boolean |
| `requiereAutenticacion` | boolean |
| `respuestaUnica` | boolean |
| `respuestasAnonimas` | boolean |
| `fechaInicio` | LocalDateTime |
| `fechaFin` | LocalDateTime |
| `codigoQr` | String |
| `preguntas` | List<PreguntaResponse> |
| `id` | Long |
| `texto` | String |
| `tipo` | TipoPregunta |
| `obligatoria` | boolean |
| `orden` | Integer |
| `opciones` | List<String> |

### `POST /api/v1/encuestas/{id}/desactivar`
**Desactivar formulario**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `id` (Long)

**Response body** (`EncuestaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigo` | String |
| `titulo` | String |
| `descripcion` | String |
| `estado` | EstadoEncuesta |
| `publico` | boolean |
| `requiereAutenticacion` | boolean |
| `respuestaUnica` | boolean |
| `respuestasAnonimas` | boolean |
| `fechaInicio` | LocalDateTime |
| `fechaFin` | LocalDateTime |
| `codigoQr` | String |
| `preguntas` | List<PreguntaResponse> |
| `id` | Long |
| `texto` | String |
| `tipo` | TipoPregunta |
| `obligatoria` | boolean |
| `orden` | Integer |
| `opciones` | List<String> |

### `DELETE /api/v1/encuestas/{id}`
**Archivar formulario**
_No se eliminan formularios con respuestas registradas (12.13)._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `id` (Long)

**Response body:** sin contenido (204/200 vacio).

### `GET /api/v1/encuestas/admin`
**Listar todos los formularios (administracion)**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')

**Response body** (Lista de `EncuestaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigo` | String |
| `titulo` | String |
| `descripcion` | String |
| `estado` | EstadoEncuesta |
| `publico` | boolean |
| `requiereAutenticacion` | boolean |
| `respuestaUnica` | boolean |
| `respuestasAnonimas` | boolean |
| `fechaInicio` | LocalDateTime |
| `fechaFin` | LocalDateTime |
| `codigoQr` | String |
| `preguntas` | List<PreguntaResponse> |
| `id` | Long |
| `texto` | String |
| `tipo` | TipoPregunta |
| `obligatoria` | boolean |
| `orden` | Integer |
| `opciones` | List<String> |

### `GET /api/v1/encuestas/{id}/estadisticas`
**Ver estadisticas de participacion de un formulario**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `id` (Long)

**Response body** (`EncuestaEstadisticasResponse`):

| Campo | Tipo |
|---|---|
| `totalRespuestas` | long |
| `resumenPorPregunta` | java.util.Map<String, Long> |

### `GET /api/v1/encuestas/{id}/respuestas`
**Ver todas las respuestas de un formulario**
_Muestra cada respuesta con el nombre de quien respondio (o \_
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `id` (Long)

**Response body** (Lista de `RespuestaEncuestaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombreRespondiente` | String |
| `fecha` | LocalDateTime |
| `respuestas` | List<ItemRespuesta> |
| `preguntaId` | Long |
| `pregunta` | String |
| `valor` | String |

### `GET /api/v1/encuestas/publicas`
**Listar formularios activos (publico)**
_Disponible sin iniciar sesion (2.7)._
- **Roles:** Publico (sin @PreAuthorize)

**Response body** (Lista de `EncuestaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigo` | String |
| `titulo` | String |
| `descripcion` | String |
| `estado` | EstadoEncuesta |
| `publico` | boolean |
| `requiereAutenticacion` | boolean |
| `respuestaUnica` | boolean |
| `respuestasAnonimas` | boolean |
| `fechaInicio` | LocalDateTime |
| `fechaFin` | LocalDateTime |
| `codigoQr` | String |
| `preguntas` | List<PreguntaResponse> |
| `id` | Long |
| `texto` | String |
| `tipo` | TipoPregunta |
| `obligatoria` | boolean |
| `orden` | Integer |
| `opciones` | List<String> |

### `GET /api/v1/encuestas/codigo/{codigo}`
**Ver un formulario por codigo**
_Usado al escanear el codigo QR del formulario (12.14). Publico._
- **Roles:** Publico (sin @PreAuthorize)
- **Path params:** `codigo` (String)

**Response body** (`EncuestaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigo` | String |
| `titulo` | String |
| `descripcion` | String |
| `estado` | EstadoEncuesta |
| `publico` | boolean |
| `requiereAutenticacion` | boolean |
| `respuestaUnica` | boolean |
| `respuestasAnonimas` | boolean |
| `fechaInicio` | LocalDateTime |
| `fechaFin` | LocalDateTime |
| `codigoQr` | String |
| `preguntas` | List<PreguntaResponse> |
| `id` | Long |
| `texto` | String |
| `tipo` | TipoPregunta |
| `obligatoria` | boolean |
| `orden` | Integer |
| `opciones` | List<String> |

### `GET /api/v1/encuestas/{id}`
- **Roles:** Publico (sin @PreAuthorize)
- **Path params:** `id` (Long)

**Response body** (`EncuestaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `codigo` | String |
| `titulo` | String |
| `descripcion` | String |
| `estado` | EstadoEncuesta |
| `publico` | boolean |
| `requiereAutenticacion` | boolean |
| `respuestaUnica` | boolean |
| `respuestasAnonimas` | boolean |
| `fechaInicio` | LocalDateTime |
| `fechaFin` | LocalDateTime |
| `codigoQr` | String |
| `preguntas` | List<PreguntaResponse> |
| `id` | Long |
| `texto` | String |
| `tipo` | TipoPregunta |
| `obligatoria` | boolean |
| `orden` | Integer |
| `opciones` | List<String> |

### `POST /api/v1/encuestas/{id}/responder`
**Responder un formulario**
_Disponible para publico o asociados segun configuracion (2.7 / 12.13)._
- **Roles:** Publico (sin @PreAuthorize)
- **Path params:** `id` (Long)

**Request body** (`ResponderEncuestaRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `nombre` | String | - |
| `respuestas` | List<RespuestaPreguntaItem> | **obligatorio**, lista no vacia |

**Response body:** sin contenido (204/200 vacio).


---

## 11. Notificaciones

**Base path:** `/api/v1/notificaciones`

### `POST /api/v1/notificaciones`
**Crear notificacion**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')

**Request body** (`NotificacionRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `titulo` | String | **obligatorio**, no vacio, maximo 200 caracteres |
| `descripcionCorta` | String | maximo 500 caracteres |
| `contenidoCompleto` | String | - |
| `tipo` | TipoNotificacion | **obligatorio** |
| `prioridad` | PrioridadNotificacion | - |
| `fechaPublicacion` | LocalDateTime | - |
| `fechaVencimiento` | LocalDateTime | - |
| `destinatarioId` | Long | - |
| `enlaceUrl` | String | - |

**Response body** (`NotificacionResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `titulo` | String |
| `descripcionCorta` | String |
| `contenidoCompleto` | String |
| `tipo` | TipoNotificacion |
| `prioridad` | PrioridadNotificacion |
| `estado` | EstadoNotificacion |
| `fechaPublicacion` | LocalDateTime |
| `fechaVencimiento` | LocalDateTime |
| `enlaceUrl` | String |
| `leida` | boolean |

### `GET /api/v1/notificaciones/publicas`
**Listar notificaciones publicas**
_Disponible sin iniciar sesion (2.7)._
- **Roles:** Publico (sin @PreAuthorize)

**Response body** (Pagina de `NotificacionResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `titulo` | String |
| `descripcionCorta` | String |
| `contenidoCompleto` | String |
| `tipo` | TipoNotificacion |
| `prioridad` | PrioridadNotificacion |
| `estado` | EstadoNotificacion |
| `fechaPublicacion` | LocalDateTime |
| `fechaVencimiento` | LocalDateTime |
| `enlaceUrl` | String |
| `leida` | boolean |

### `GET /api/v1/notificaciones/mis-notificaciones`
**Listar mis notificaciones**
_Notificaciones personales del usuario autenticado (13.11)._
- **Roles:** Publico (sin @PreAuthorize)

**Response body** (Pagina de `NotificacionResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `titulo` | String |
| `descripcionCorta` | String |
| `contenidoCompleto` | String |
| `tipo` | TipoNotificacion |
| `prioridad` | PrioridadNotificacion |
| `estado` | EstadoNotificacion |
| `fechaPublicacion` | LocalDateTime |
| `fechaVencimiento` | LocalDateTime |
| `enlaceUrl` | String |
| `leida` | boolean |

### `PATCH /api/v1/notificaciones/{id}/leida`
**Marcar notificacion como leida**
- **Roles:** Publico (sin @PreAuthorize)
- **Path params:** `id` (Long)

**Response body:** sin contenido (204/200 vacio).

### `DELETE /api/v1/notificaciones/{id}`
**Eliminar notificacion definitivamente**
_Exclusivo del Administrador (13.12)._
- **Roles:** hasRole('ADMINISTRADOR')
- **Path params:** `id` (Long)

**Response body:** sin contenido (204/200 vacio).


---

## 12. Portal Publico

**Base path:** `/api/v1/publicaciones`

### `POST /api/v1/publicaciones`
**Crear publicacion**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')

**Request body** (`PublicacionRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `titulo` | String | **obligatorio**, no vacio, maximo 200 caracteres |
| `descripcionCorta` | String | maximo 500 caracteres |
| `contenidoCompleto` | String | - |
| `imagenUrl` | String | - |
| `posicionImagen` | String | maximo 15 caracteres |
| `categoriaId` | Long | - |
| `etiquetasIds` | List<Long> | - |
| `destacada` | boolean | - |

**Response body** (`PublicacionResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `titulo` | String |
| `descripcionCorta` | String |
| `contenidoCompleto` | String |
| `imagenUrl` | String |
| `posicionImagen` | String |
| `categoria` | String |
| `etiquetas` | List<String> |
| `autor` | String |
| `estado` | EstadoPublicacion |
| `destacada` | boolean |
| `fechaCreacion` | LocalDateTime |

### `POST /api/v1/publicaciones/{id}/publicar`
**Publicar contenido**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `id` (Long)

**Response body** (`PublicacionResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `titulo` | String |
| `descripcionCorta` | String |
| `contenidoCompleto` | String |
| `imagenUrl` | String |
| `posicionImagen` | String |
| `categoria` | String |
| `etiquetas` | List<String> |
| `autor` | String |
| `estado` | EstadoPublicacion |
| `destacada` | boolean |
| `fechaCreacion` | LocalDateTime |

### `POST /api/v1/publicaciones/{id}/ocultar`
**Ocultar contenido**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `id` (Long)

**Response body** (`PublicacionResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `titulo` | String |
| `descripcionCorta` | String |
| `contenidoCompleto` | String |
| `imagenUrl` | String |
| `posicionImagen` | String |
| `categoria` | String |
| `etiquetas` | List<String> |
| `autor` | String |
| `estado` | EstadoPublicacion |
| `destacada` | boolean |
| `fechaCreacion` | LocalDateTime |

### `PATCH /api/v1/publicaciones/{id}/destacar`
**Destacar/quitar destacado**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `id` (Long)
- **Query params:** `destacada` (boolean, requerido)

**Response body** (`PublicacionResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `titulo` | String |
| `descripcionCorta` | String |
| `contenidoCompleto` | String |
| `imagenUrl` | String |
| `posicionImagen` | String |
| `categoria` | String |
| `etiquetas` | List<String> |
| `autor` | String |
| `estado` | EstadoPublicacion |
| `destacada` | boolean |
| `fechaCreacion` | LocalDateTime |

### `PUT /api/v1/publicaciones/{id}`
**Editar publicacion**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `id` (Long)

**Request body** (`PublicacionRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `titulo` | String | **obligatorio**, no vacio, maximo 200 caracteres |
| `descripcionCorta` | String | maximo 500 caracteres |
| `contenidoCompleto` | String | - |
| `imagenUrl` | String | - |
| `posicionImagen` | String | maximo 15 caracteres |
| `categoriaId` | Long | - |
| `etiquetasIds` | List<Long> | - |
| `destacada` | boolean | - |

**Response body** (`PublicacionResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `titulo` | String |
| `descripcionCorta` | String |
| `contenidoCompleto` | String |
| `imagenUrl` | String |
| `posicionImagen` | String |
| `categoria` | String |
| `etiquetas` | List<String> |
| `autor` | String |
| `estado` | EstadoPublicacion |
| `destacada` | boolean |
| `fechaCreacion` | LocalDateTime |

### `DELETE /api/v1/publicaciones/{id}`
**Eliminar publicacion definitivamente**
_Exclusivo del Administrador (11.10)._
- **Roles:** hasRole('ADMINISTRADOR')
- **Path params:** `id` (Long)

**Response body:** sin contenido (204/200 vacio).

### `GET /api/v1/publicaciones/admin`
**Listado completo para el panel de edicion**
_Incluye borradores y ocultas, no solo publicadas. Tesorero/Administrador._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')

**Response body** (Pagina de `PublicacionResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `titulo` | String |
| `descripcionCorta` | String |
| `contenidoCompleto` | String |
| `imagenUrl` | String |
| `posicionImagen` | String |
| `categoria` | String |
| `etiquetas` | List<String> |
| `autor` | String |
| `estado` | EstadoPublicacion |
| `destacada` | boolean |
| `fechaCreacion` | LocalDateTime |

### `GET /api/v1/publicaciones/{id}`
**Ver el detalle de una publicacion**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `id` (Long)

**Response body** (`PublicacionResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `titulo` | String |
| `descripcionCorta` | String |
| `contenidoCompleto` | String |
| `imagenUrl` | String |
| `posicionImagen` | String |
| `categoria` | String |
| `etiquetas` | List<String> |
| `autor` | String |
| `estado` | EstadoPublicacion |
| `destacada` | boolean |
| `fechaCreacion` | LocalDateTime |

### `GET /api/v1/publicaciones/publicas`
**Galeria publica**
_Publicaciones publicadas, disponible sin login (11.5)._
- **Roles:** Publico (sin @PreAuthorize)

**Response body** (Pagina de `PublicacionResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `titulo` | String |
| `descripcionCorta` | String |
| `contenidoCompleto` | String |
| `imagenUrl` | String |
| `posicionImagen` | String |
| `categoria` | String |
| `etiquetas` | List<String> |
| `autor` | String |
| `estado` | EstadoPublicacion |
| `destacada` | boolean |
| `fechaCreacion` | LocalDateTime |

### `GET /api/v1/publicaciones/destacadas`
**Publicaciones destacadas**
_Disponible sin login (11.6)._
- **Roles:** Publico (sin @PreAuthorize)

**Response body** (Lista de `PublicacionResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `titulo` | String |
| `descripcionCorta` | String |
| `contenidoCompleto` | String |
| `imagenUrl` | String |
| `posicionImagen` | String |
| `categoria` | String |
| `etiquetas` | List<String> |
| `autor` | String |
| `estado` | EstadoPublicacion |
| `destacada` | boolean |
| `fechaCreacion` | LocalDateTime |

### `POST /api/v1/publicaciones/categorias`
**Crear categoria**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')

**Request body** (`CategoriaRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `nombre` | String | **obligatorio**, no vacio |

**Response body** (`Categoria`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombre` | String (max 60) |
| `createdAt/updatedAt` | LocalDateTime (heredado de BaseEntity) |

### `GET /api/v1/publicaciones/categorias`
**Listar categorias**
- **Roles:** Publico (sin @PreAuthorize)

**Response body** (Lista de `Categoria`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombre` | String (max 60) |
| `createdAt/updatedAt` | LocalDateTime (heredado de BaseEntity) |

### `POST /api/v1/publicaciones/etiquetas`
**Crear etiqueta**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')

**Request body** (`EtiquetaRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `nombre` | String | **obligatorio**, no vacio |
| `color` | String | - |

**Response body** (`Etiqueta`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombre` | String (max 60) |
| `color` | String (max 10, opcional) |

### `GET /api/v1/publicaciones/etiquetas`
**Listar etiquetas**
- **Roles:** Publico (sin @PreAuthorize)

**Response body** (Lista de `Etiqueta`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombre` | String (max 60) |
| `color` | String (max 10, opcional) |

### `POST /api/v1/publicaciones/videos`
**Publicar video**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')

**Request body** (`VideoRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `titulo` | String | **obligatorio**, no vacio, maximo 200 caracteres |
| `descripcion` | String | - |
| `urlVideo` | String | **obligatorio**, no vacio |

**Response body** (`Video`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `titulo` | String (max 200) |
| `descripcion` | String (sin limite - TEXT) |
| `urlVideo` | String (sin limite - TEXT) |
| `visible` | boolean |

### `GET /api/v1/publicaciones/videos/publicos`
**Listar videos publicos**
- **Roles:** Publico (sin @PreAuthorize)

**Response body** (Lista de `Video`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `titulo` | String (max 200) |
| `descripcion` | String (sin limite - TEXT) |
| `urlVideo` | String (sin limite - TEXT) |
| `visible` | boolean |

### `PATCH /api/v1/publicaciones/videos/{id}/ocultar`
**Ocultar video**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')
- **Path params:** `id` (Long)

**Response body:** sin contenido (204/200 vacio).

### `GET /api/v1/publicaciones/{id}/reacciones`
**Ver reacciones de una publicacion**
_Conteo total por cada emoji. Publico (11.x)._
- **Roles:** Publico (sin @PreAuthorize)
- **Path params:** `id` (Long)

**Response body** (Lista de `ReaccionResponse`):

| Campo | Tipo |
|---|---|
| `emoji` | String |
| `contador` | long |

### `POST /api/v1/publicaciones/{id}/reacciones`
**Reaccionar a una publicacion**
_Suma 1 al contador del emoji indicado. Publico, no requiere sesion. _
- **Roles:** Publico (sin @PreAuthorize)
- **Path params:** `id` (Long)
- **Query params:** `emoji` (String, requerido)

**Response body** (`ReaccionResponse`):

| Campo | Tipo |
|---|---|
| `emoji` | String |
| `contador` | long |

### `DELETE /api/v1/publicaciones/{id}/reacciones`
**Quitar una reaccion propia**
_Resta 1 al contador del emoji indicado (nunca baja de cero). Publico._
- **Roles:** Publico (sin @PreAuthorize)
- **Path params:** `id` (Long)
- **Query params:** `emoji` (String, requerido)

**Response body:** sin contenido (204/200 vacio).


---

## 13. Auditoria

**Base path:** `/api/v1/auditoria`

### `GET /api/v1/auditoria`
**Listar toda la auditoria**
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)

**Response body** (Pagina de `AuditoriaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `usuario` | String |
| `fecha` | LocalDateTime |
| `accion` | String |
| `modulo` | String |
| `registroAfectado` | String |
| `observaciones` | String |

### `GET /api/v1/auditoria/modulo/{modulo}`
**Filtrar auditoria por modulo**
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)
- **Path params:** `modulo` (String)

**Response body** (Pagina de `AuditoriaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `usuario` | String |
| `fecha` | LocalDateTime |
| `accion` | String |
| `modulo` | String |
| `registroAfectado` | String |
| `observaciones` | String |

### `GET /api/v1/auditoria/usuario/{usuario}`
**Filtrar auditoria por usuario**
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)
- **Path params:** `usuario` (String)

**Response body** (Pagina de `AuditoriaResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `usuario` | String |
| `fecha` | LocalDateTime |
| `accion` | String |
| `modulo` | String |
| `registroAfectado` | String |
| `observaciones` | String |

### `GET /api/v1/auditoria/estado`
**Consultar si la auditoria esta activa**
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)

**Response body** (`EstadoAuditoria`):

| Campo | Tipo |
|---|---|
| `activa` | boolean |

### `PATCH /api/v1/auditoria/desactivar`
**Desactivar la auditoria**
_Deja de registrar nuevas acciones. Esta misma accion siempre queda registrada como el ultimo movimiento, indicando quien la solicito._
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)

**Response body:** sin contenido (204/200 vacio).

### `PATCH /api/v1/auditoria/activar`
**Reactivar la auditoria**
- **Roles:** hasRole('ADMINISTRADOR')  (definido a nivel de clase)

**Response body:** sin contenido (204/200 vacio).


---

## 14. Estadisticas / Dashboard

**Base path:** `/api/v1/estadisticas`

### `GET /api/v1/estadisticas/dashboard`
**Dashboard general del sistema**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)


---

## 15. Consultas Publicas

**Base path:** `/api/v1/consultas`

### `GET /api/v1/consultas/estado-servicio`
**Consultar estado del servicio por documento**
_Consulta publica minima: confirma si el documento esta registrado y el estado del servicio._
- **Roles:** Publico (sin @PreAuthorize)
- **Query params:** `documento` (String, requerido)

**Response body** (`EstadoServicioPublicoResponse`):

| Campo | Tipo |
|---|---|
| `codigoInterno` | String |
| `nombreCompleto` | String |
| `estadoServicio` | EstadoServicio (ACTIVO/SUSPENDIDO/INACTIVO) |


---

## 16. Informes

**Base path:** `/api/v1/informes`

### `GET /api/v1/informes/periodo/mes/{mesContableId}`
**Datos del informe mensual (JSON)**
_Util para previsualizar los datos antes de generar el documento._
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)
- **Path params:** `mesContableId` (Long)

**Response body** (`InformePeriodoResponse`):

| Campo | Tipo |
|---|---|
| `tipoInforme` | String |
| `tituloPeriodo` | String |
| `fechaGeneracion` | LocalDate |
| `totalIngresos` | BigDecimal |
| `totalGastos` | BigDecimal |
| `balance` | BigDecimal |
| `facturasGeneradas` | long |
| `facturasPagadas` | long |
| `facturasPendientes` | long |
| `facturasVencidas` | long |
| `facturasAnuladas` | long |
| `totalFacturado` | BigDecimal |
| `totalRecaudadoFacturas` | BigDecimal |
| `numeroMultas` | long |
| `totalMultas` | BigDecimal |
| `totalM3Consumidos` | long |
| `promedioConsumoM3` | double |
| `asociadosActivos` | long |
| `asociadosSuspendidos` | long |
| `facturas` | List<FacturaResumenItem> |
| `movimientos` | List<MovimientoResumenItem> |
| `numeroFactura` | String |
| `asociadoNombre` | String |
| `fechaEmision` | LocalDate |
| `total` | BigDecimal |
| `estado` | String |
| `numero` | String |
| `tipo` | String |
| `concepto` | String |
| `valor` | BigDecimal |
| `fecha` | LocalDate |

### `GET /api/v1/informes/periodo/mes/{mesContableId}/html`
**Informe mensual en HTML**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)
- **Path params:** `mesContableId` (Long)

**Response body:** texto plano/HTML (`text/html`).

### `GET /api/v1/informes/periodo/mes/{mesContableId}/pdf`
**Informe mensual en PDF**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)
- **Path params:** `mesContableId` (Long)

**Response body:** archivo binario (PDF, `application/pdf`).

### `GET /api/v1/informes/periodo/anio/{anioContableId}`
**Datos del informe anual (JSON)**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)
- **Path params:** `anioContableId` (Long)

**Response body** (`InformePeriodoResponse`):

| Campo | Tipo |
|---|---|
| `tipoInforme` | String |
| `tituloPeriodo` | String |
| `fechaGeneracion` | LocalDate |
| `totalIngresos` | BigDecimal |
| `totalGastos` | BigDecimal |
| `balance` | BigDecimal |
| `facturasGeneradas` | long |
| `facturasPagadas` | long |
| `facturasPendientes` | long |
| `facturasVencidas` | long |
| `facturasAnuladas` | long |
| `totalFacturado` | BigDecimal |
| `totalRecaudadoFacturas` | BigDecimal |
| `numeroMultas` | long |
| `totalMultas` | BigDecimal |
| `totalM3Consumidos` | long |
| `promedioConsumoM3` | double |
| `asociadosActivos` | long |
| `asociadosSuspendidos` | long |
| `facturas` | List<FacturaResumenItem> |
| `movimientos` | List<MovimientoResumenItem> |
| `numeroFactura` | String |
| `asociadoNombre` | String |
| `fechaEmision` | LocalDate |
| `total` | BigDecimal |
| `estado` | String |
| `numero` | String |
| `tipo` | String |
| `concepto` | String |
| `valor` | BigDecimal |
| `fecha` | LocalDate |

### `GET /api/v1/informes/periodo/anio/{anioContableId}/html`
**Informe anual en HTML**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)
- **Path params:** `anioContableId` (Long)

**Response body:** texto plano/HTML (`text/html`).

### `GET /api/v1/informes/periodo/anio/{anioContableId}/pdf`
**Informe anual en PDF**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)
- **Path params:** `anioContableId` (Long)

**Response body:** archivo binario (PDF, `application/pdf`).

### `GET /api/v1/informes/asociado/{asociadoId}`
**Datos del informe de seguimiento a un asociado (JSON)**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)
- **Path params:** `asociadoId` (Long)

**Response body** (`InformeAsociadoResponse`):

| Campo | Tipo |
|---|---|
| `fechaGeneracion` | LocalDate |
| `codigoInterno` | String |
| `documento` | String |
| `nombreCompleto` | String |
| `telefonoPrincipal` | String |
| `direccion` | String |
| `estadoServicio` | String |
| `fechaAfiliacion` | LocalDate |
| `numeroMedidor` | String |
| `totalFacturas` | long |
| `facturasPagadas` | long |
| `facturasPendientes` | long |
| `facturasVencidas` | long |
| `totalFacturado` | BigDecimal |
| `totalPagado` | BigDecimal |
| `totalPendiente` | BigDecimal |
| `numeroMultas` | long |
| `totalMultas` | BigDecimal |
| `facturas` | List<FacturaResumenItem> |
| `pagos` | List<PagoResumenItem> |
| `multas` | List<MultaResumenItem> |
| `numeroFactura` | String |
| `fechaEmision` | LocalDate |
| `total` | BigDecimal |
| `saldoPendiente` | BigDecimal |
| `estado` | String |
| `numeroRecibo` | String |
| `numeroFactura` | String |
| `valor` | BigDecimal |
| `fecha` | LocalDateTime |
| `metodoPago` | String |
| `motivo` | String |
| `valor` | BigDecimal |
| `fecha` | LocalDate |
| `estado` | String |

### `GET /api/v1/informes/asociado/{asociadoId}/html`
**Informe de seguimiento a un asociado en HTML**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)
- **Path params:** `asociadoId` (Long)

**Response body:** texto plano/HTML (`text/html`).

### `GET /api/v1/informes/asociado/{asociadoId}/pdf`
**Informe de seguimiento a un asociado en PDF**
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')  (definido a nivel de clase)
- **Path params:** `asociadoId` (Long)

**Response body:** archivo binario (PDF, `application/pdf`).


---

## 17. Reportes Ciudadanos

**Base path:** ``

### `POST /api/v1/publico/reportes`
**Reportar una fuga o enviar una queja/reclamo**
_Publico, sin necesidad de iniciar sesion. Nombre y mensaje son obligatorios; el contacto (telefono o correo) es opcional. _
- **Roles:** Publico (sin @PreAuthorize)

**Request body** (`ReporteCiudadanoRequest`):

| Campo | Tipo | Validaciones |
|---|---|---|
| `nombre` | String | **obligatorio**, no vacio, maximo 150 caracteres |
| `mensaje` | String | **obligatorio**, no vacio |
| `contacto` | String | maximo 150 caracteres |

**Response body** (`ReporteCiudadanoResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombre` | String |
| `mensaje` | String |
| `contacto` | String |
| `fechaCreacion` | LocalDateTime |
| `fechaEliminacion` | LocalDateTime |

### `GET /api/v1/reportes`
**Listar todos los reportes ciudadanos**
_Exclusivo del Administrador y el Tesorero. Muestra nombre, mensaje, contacto, _
- **Roles:** hasRole('ADMINISTRADOR') or hasRole('TESORERO')

**Response body** (Pagina de `ReporteCiudadanoResponse`):

| Campo | Tipo |
|---|---|
| `id` | Long |
| `nombre` | String |
| `mensaje` | String |
| `contacto` | String |
| `fechaCreacion` | LocalDateTime |
| `fechaEliminacion` | LocalDateTime |

---

## Apendice: Enums del dominio

Estos son los valores exactos (`String` en JSON) que acepta o devuelve cada enum. Enviar cualquier otro valor produce un 400 de "Error de validacion" (JSON invalido para ese tipo).

| Enum | Valores |
|---|---|
| `EstadoAnio` | `ACTIVO`, `CERRADO`, `ARCHIVADO` |
| `EstadoEncuesta` | `BORRADOR`, `ACTIVA`, `FINALIZADA`, `ARCHIVADA` |
| `EstadoFactura` | `PENDIENTE`, `PAGADA_PARCIAL`, `PAGADA`, `VENCIDA`, `ANULADA` |
| `EstadoMedidor` | `ACTIVO`, `EN_MANTENIMIENTO`, `DANADO`, `RETIRADO` |
| `EstadoMes` | `ABIERTO`, `CERRADO`, `REABIERTO` |
| `EstadoMulta` | `PENDIENTE`, `PAGADA`, `ANULADA` |
| `EstadoNotificacion` | `ACTIVA`, `OCULTA`, `PROGRAMADA` |
| `EstadoPublicacion` | `BORRADOR`, `PUBLICADA`, `OCULTA` |
| `EstadoRecibo` | `EMITIDO`, `ANULADO` |
| `EstadoServicio` | `ACTIVO`, `SUSPENDIDO`, `INACTIVO` |
| `FuenteArchivoInstitucional` | `STORAGE`, `URL` |
| `PrioridadNotificacion` | `BAJA`, `NORMAL`, `MEDIA`, `ALTA`, `URGENTE` |
| `Rol` | `ASOCIADO`, `TESORERO`, `ADMINISTRADOR` |
| `TipoArchivoInstitucional` | `LOGO`, `FIRMA`, `SELLO` |
| `TipoDocumento` | `CC`, `CE`, `TI`, `NIT`, `PASAPORTE` |
| `TipoMovimiento` | `ENTRADA`, `SALIDA` |
| `TipoNotificacion` | `PUBLICA`, `ASOCIADO`, `ADMINISTRATIVA` |
| `TipoPregunta` | `TEXTO_CORTO`, `TEXTO_LARGO`, `OPCION_UNICA`, `OPCION_MULTIPLE`, `ESCALA`, `SI_NO` |

