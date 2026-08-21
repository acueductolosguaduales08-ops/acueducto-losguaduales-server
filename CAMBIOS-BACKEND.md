# Cambios en el backend — Acueducto Los Guaduales

Documento único con todo lo agregado y modificado en esta ronda, pensado para pasarlo al
frontend. Está organizado por módulo, con los endpoints nuevos/cambiados, el comportamiento
esperado y notas específicas para la UI donde aplica.

> Base de la API: `/api/v1`. Todo lo que dice "Administrador o Tesorero" ya lo puede llamar
> cualquiera de esos dos roles con su token normal — no hay nada especial que configurar.

---

## Índice

1. [Hero / Banner (módulo nuevo)](#1-hero--banner-módulo-nuevo)
2. [Lecturas: lectura anterior automática y manual](#2-lecturas-lectura-anterior-automática-y-manual)
3. [Notificaciones automáticas](#3-notificaciones-automáticas)
4. [Reportes ciudadanos (avisos)](#4-reportes-ciudadanos-avisos)
5. [Módulo nuevo: Gestión de datos importantes](#5-módulo-nuevo-gestión-de-datos-importantes)
6. [Asociados: teléfono opcional y estado de cuenta](#6-asociados-teléfono-opcional-y-estado-de-cuenta)
7. [Multas independientes ("aparte")](#7-multas-independientes-aparte)
8. [Listado de cuentas de usuario](#8-listado-de-cuentas-de-usuario)
9. [Formularios: programación automática y manual](#9-formularios-programación-automática-y-manual)
10. [Formularios: configuración general al crear](#10-formularios-configuración-general-al-crear)
11. [Infraestructura: Render + Supabase](#11-infraestructura-render--supabase)
12. [Portal Público: eliminación definitiva de etiquetas](#12-portal-público-eliminación-definitiva-de-etiquetas)
13. [Corrección: 500 en configuracion/hero por columnas faltantes](#13-corrección-500-en-configuracionhero-por-columnas-faltantes)
14. [Tabla resumen de endpoints nuevos/modificados](#14-tabla-resumen-de-endpoints-nuevosmodificados)
15. [Listado general de asociados (HTML/PDF)](#15-listado-general-de-asociados-htmlpdf)
16. [Chat interno (módulo nuevo)](#16-chat-interno-módulo-nuevo)
17. [Enlace público de descarga de facturas y recibos](#17-enlace-público-de-descarga-de-facturas-y-recibos)
18. [Corrección: Swagger "Failed to fetch" en Render](#18-corrección-swagger-failed-to-fetch-en-render)
19. [Bloqueo de cuentas de usuario](#19-bloqueo-de-cuentas-de-usuario)

---

## 1. Hero / Banner (módulo nuevo)

Reemplaza por completo la idea original de "un solo link". Ahora se pueden registrar **varios**
heros, y el sistema decide cuál mostrar según un **modo**:

- **`UNICO`**: siempre se muestra el que está marcado como *principal*.
- **`ALEATORIO_15MIN`**: se muestra uno al azar de los registrados, y **cambia cada 15
  minutos**.

### Nota técnica importante (por qué el modo aleatorio es confiable)

El modo "cambia cada 15 minutos" **no** depende de una tarea corriendo en segundo plano todo el
tiempo. El proyecto está en el plan gratuito de Render, que apaga el servicio tras ~15 minutos
sin tráfico — si dependiera de un temporizador de fondo, dejaría de rotar cada vez que el
servicio se duerme. En cambio, el hero "vigente" se calcula **en el momento en que alguien lo
consulta** (se guarda cuál tocó y desde cuándo), así que el resultado es siempre correcto sin
importar si el servicio estuvo dormido en el medio.

### Endpoints

| Método | Ruta | Quién | Descripción |
|---|---|---|---|
| `GET` | `/configuracion/hero/actual` | **Público** (sin login) | El link que corresponde mostrar ahora mismo. `{ "link": "...", "modo": "UNICO" }`. `link` puede venir `null` si todavía no hay ninguno registrado. |
| `POST` | `/configuracion/hero` | Admin o Tesorero | Agregar un hero nuevo. Body: `{ "link": "..." }`. El link va **en el body**, no en la URL, así que acepta links largos y con caracteres especiales sin problema. El primero que se agrega queda como principal automáticamente. |
| `GET` | `/configuracion/hero` | Admin o Tesorero | Listar todos los heros registrados, con su `id`, `link` y si es `principal`. |
| `DELETE` | `/configuracion/hero/{id}` | Admin o Tesorero | Borra un hero definitivamente. Si era el principal, se promueve otro automáticamente (si queda alguno). |
| `PATCH` | `/configuracion/hero/{id}/principal` | Admin o Tesorero | Marca ese hero como principal. Solo tiene efecto visible cuando el modo activo es `UNICO`. |
| `PUT` | `/configuracion/hero/modo` | Admin o Tesorero | Cambia el modo. Body: `{ "modo": "UNICO" }` o `{ "modo": "ALEATORIO_15MIN" }`. |

### Para el frontend

- La pantalla pública solo necesita `GET /configuracion/hero/actual`. Como puede cambiar cada 15
  minutos en modo aleatorio, conviene refrescarlo periódicamente (por ejemplo cada 1-5 min) en
  vez de pedirlo una sola vez al cargar.
- La pantalla de administración de heros necesita: listar (con miniatura/preview de cada link),
  botón de agregar, botón de eliminar por ítem, marcar como principal, y un selector de modo
  (único / aleatorio).

---

## 2. Lecturas: lectura anterior automática y manual

*(Recapitulando lo ya entregado antes, para que quede todo junto en un solo documento.)*

`POST /lecturas` y `PUT /lecturas/{id}` siguen recibiendo el mismo body de siempre:

```json
{
  "medidorId": 0,
  "mesContableId": 0,
  "fechaLectura": "2026-08-09",
  "lecturaActual": 0,
  "observaciones": "string"
}
```

Ahora aceptan además un campo **opcional**:

```json
{
  "...": "...",
  "lecturaAnterior": 120
}
```

- **Si no se envía `lecturaAnterior`** (o se envía `null`): el sistema la calcula solo, tomando
  la última lectura registrada de ese medidor. Este es el comportamiento de siempre, sin
  cambios.
- **Si se envía `lecturaAnterior`**: se usa ese valor tal cual, en vez de calcularlo. Pensado
  para cuando se saltó un período y el último registro ya no refleja la lectura real anterior.

Las dos formas conviven en el mismo campo y el mismo endpoint — no hay un endpoint separado para
"modo manual".

**Importante para el frontend (punto 13 del pedido):** cuando el usuario esté en el flujo donde
puede indicar `lecturaAnterior` manualmente, el cálculo en tiempo real de m³ consumidos y valor a
pagar que ya existe en el formulario debe usar el valor que el usuario escribió a mano en ese
momento (no esperar a la respuesta del servidor) — es decir, extender el mismo cálculo que ya
tienen hoy para que reaccione también al campo manual, no solo al automático.

En `PUT /lecturas/{id}` (editar), si se envía `lecturaAnterior` en el body, **se ignora**: ese
valor queda fijo desde el registro original y no se puede editar después, para no desincronizar
el consumo ya calculado.

### Endpoint nuevo: `GET /lecturas/anterior-por-defecto` (exclusivo del Administrador)

Antes de registrar una lectura, el formulario puede preguntarle al backend **cuál será la lectura
anterior que se usará por defecto** para ese medidor, sin necesidad de guardar nada:

```
GET /api/v1/lecturas/anterior-por-defecto?medidorId=5
```

Devuelve exactamente el valor que tomará `lecturaAnterior` al crear la lectura: la **lectura
actual del último registro** del medidor, o `0` si todavía no tiene historial (misma regla que ya
aplicaba `POST /lecturas`). Ejemplo de respuesta:

```json
{
  "medidorId": 5,
  "numeroMedidor": "M-00005",
  "asociadoId": 3,
  "asociadoNombre": "Nombre Apellido",
  "lecturaAnteriorPorDefecto": 120,
  "ultimaLecturaId": 42,
  "ultimaFechaLectura": "2026-07-05",
  "hayRegistroPrevio": true
}
```

- Si el medidor **no tiene lecturas previas**, viene `lecturaAnteriorPorDefecto: 0` y
  `hayRegistroPrevio: false` (y `ultimaLecturaId`/`ultimaFechaLectura` en `null`).
- El medidor se identifica por su **id** (el que se manda en `POST /lecturas` → `medidorId`).
- El campo `asociadoNombre`/`asociadoId` puede venir `null` si el medidor aún no tiene asociado
  asignado (igual que al registrar, que exigiría asociado).

**Para el frontend:** al abrir el formulario de "registrar lectura" (o al cambiar de medidor),
llamar este endpoint y mostrar el valor devuelto como la "lectura anterior" precargada, para que
el usuario la confirme o la corrija manualmente antes de enviar.

---

## 3. Notificaciones automáticas

Se revisó cuáles eventos ya disparaban una notificación al asociado y cuáles no. Antes de esta
ronda, **solo** existían notificaciones automáticas para: factura generada y pago registrado.

### Agregado en esta ronda

| Evento | Se notifica |
|---|---|
| Cambio de estado del servicio (incluye suspensión) | ✅ Nuevo |
| Factura anulada | ✅ Nuevo |
| Factura marcada vencida (automático, vencimiento de plazo) | ✅ Nuevo |
| Se registra una multa | ✅ Nuevo |
| Cambio de contraseña (aviso de seguridad) | ✅ Nuevo |

### No se agregó (y por qué)

**Cancelar/anular un recibo**: se verificó y esa función **no existe todavía en el sistema** —
el estado `ANULADO` está definido en el modelo de datos pero ningún endpoint lo usa hoy. No se
implementó como parte de esta ronda por ser una funcionalidad nueva completa (con sus propias
reglas de negocio sobre qué pasa con el pago y la factura asociados), no solo "agregar una
notificación". Si la quieren, es un punto aparte a definir.

### Para el frontend (pedido explícito del punto 2)

Documentando lo que pidieron agregar a la documentación:

- **Marcar como leída / eliminar una notificación "en local"**: si el objetivo es que el usuario
  no vuelva a ver la misma notificación repetida, eso ya es soportado por el backend en forma
  persistente: `PATCH /notificaciones/{id}/leer` (o el endpoint equivalente ya existente) marca
  la notificación como leída para ese usuario específico, y queda guardado en el servidor —no
  hace falta guardar nada en local storage del navegador para lograr ese efecto, el backend ya
  lo persiste por usuario. Si el frontend igual quiere una capa adicional de "ocultar
  localmente sin marcar como leída en servidor" (por ejemplo para descartar de la vista sin
  necesariamente confirmar lectura), **esa sí sería una opción nueva a nivel de frontend
  únicamente** — está bien como mejora, pero es una decisión de UI, no algo que el backend
  necesite soportar.

---

## 4. Reportes ciudadanos (avisos)

`POST /reportes` (público, cualquier persona) ahora acepta un campo opcional:

```json
{
  "nombre": "...",
  "mensaje": "...",
  "contacto": "...",
  "imagenUrl": "https://..."
}
```

- `imagenUrl` es opcional. Si se envía, se normaliza igual que cualquier otro link largo del
  sistema (acepta URLs largas y con caracteres especiales sin romper el guardado; debe empezar
  con `http://` o `https://`).
- **Para el frontend**: al mostrar un reporte con `imagenUrl`, agregar una vista previa de la
  imagen (thumbnail) en vez de solo el link en texto — así se pidió explícitamente.

### Borrado definitivo

`DELETE /reportes/{id}` — exclusivo del Administrador. Borra el reporte de inmediato, sin
esperar los 8 días de retención automática que ya existían (esa retención automática **se
mantiene igual**, esto es una opción adicional para casos como spam o contenido inapropiado).

---

## 5. Módulo nuevo: Gestión de datos importantes

Módulo completo nuevo, exclusivo del **Administrador**, para eliminación **definitiva e
irreversible** de todos los tipos de datos del sistema. Base: `/api/v1/datos-importantes`.

### ⚠️ Actualización de esta ronda — borrado en CASCADA total

Antes, el módulo solo cubría 7 tipos y varios quedaban **bloqueados** si tenían datos
relacionados. Ahora el borrado es **en cascada**: si el registro que se quiere borrar tiene
historial, ese historial también se borra automáticamente (en el orden correcto para no romper
las referencias), así que **ningún tipo queda bloqueado** por tener datos asociados.

Reglas clave de la cascada:

- **Al borrar un asociado** se borra TODO su historial (facturas → pagos → recibos → movimientos,
  multas, lecturas y su cuenta de usuario si existe) y el **medidor NO se borra**: solo se
  desvincula y queda disponible.
- **Al borrar un medidor** se desvincula del asociado (el asociado no se borra) y se borran sus
  lecturas en cascada (y las facturas que esas lecturas hayan generado).
- **Al borrar una factura** se borran en cascada sus pagos, recibos, movimientos y conceptos; las
  multas que apuntaban a ella quedan sueltas y su lectura vuelve a quedar disponible.
- **Al borrar un pago** se borra su recibo y movimientos y se **recalcula** el total pagado y el
  estado de la factura (reversa contable real).
- **Al borrar una multa** que ya estaba incluida en una factura, se descuenta su valor del total
  de esa factura.
- **Al borrar una cuenta** se borran en cascada los formularios que creó, sus notificaciones,
  respuestas de formularios (se desvinculan), pagos que registró como tesorero y movimientos que
  registró. Única excepción: **no se puede borrar la propia cuenta en uso** (dejaría el sistema
  sin el administrador actual; use otra cuenta).
- **Al borrar un periodo contable (mes)** se borran en cascada sus facturas, lecturas y
  movimientos; **al borrar un año contable** se borran sus meses en cascada.
- **Al borrar un formulario** se borran sus preguntas y respuestas; **al borrar un recibo** se
  borran sus movimientos (el pago y la factura no se tocan); **al borrar una notificación** se
  borran sus registros de lectura.

### Cómo funciona cada endpoint

Todos los `DELETE` piden la **contraseña del Administrador en el body**, aunque ya tenga sesión
iniciada:

```json
{ "password": "..." }
```

| Tipo | Ruta |
|---|---|
| Formulario | `DELETE /datos-importantes/formularios/{id}` |
| Factura | `DELETE /datos-importantes/facturas/{id}` |
| Recibo | `DELETE /datos-importantes/recibos/{id}` |
| Asociado | `DELETE /datos-importantes/asociados/{id}` |
| Cuenta | `DELETE /datos-importantes/cuentas/{id}` |
| Periodo contable (mes) | `DELETE /datos-importantes/periodos-contables/{id}` |
| Año contable | `DELETE /datos-importantes/anios-contables/{id}` |
| Multa | `DELETE /datos-importantes/multas/{id}` |
| Medidor | `DELETE /datos-importantes/medidores/{id}` |
| Lectura | `DELETE /datos-importantes/lecturas/{id}` |
| Pago | `DELETE /datos-importantes/pagos/{id}` |
| Movimiento de tesorería | `DELETE /datos-importantes/movimientos/{id}` |
| Notificación | `DELETE /datos-importantes/notificaciones/{id}` |

### Antes de borrar: `GET /verificar` (informa qué se va a borrar)

`GET /datos-importantes/verificar?tipo=ASOCIADO&id=5` devuelve, **sin borrar nada**, qué se va a
eliminar y en cascada. `tipo` acepta: `FORMULARIO`, `FACTURA`, `RECIBO`, `ASOCIADO`, `CUENTA`,
`PERIODO_CONTABLE`, `ANIO_CONTABLE`, `MULTA`, `MEDIDOR`, `LECTURA`, `PAGO`, `MOVIMIENTO`,
`NOTIFICACION`.

Ejemplo de respuesta:

```json
{
  "tipo": "ASOCIADO",
  "id": 5,
  "borrable": true,
  "referencia": "ASC-00005",
  "motivosBloqueo": [],
  "cascada": {
    "facturas": 12,
    "pagos": 10,
    "recibos": 10,
    "multas": 2,
    "lecturas": 14,
    "movimientosTesoreria": 10,
    "notificaciones": 8,
    "cuentaVinculada": 1,
    "medidorDesvinculado": 1
  },
  "mensaje": "Se borrará el asociado y TODO su historial en cascada. El medidor solo se desvincula, NO se borra."
}
```

Cuando algo no se puede borrar (el único caso es la propia cuenta en uso), `borrable` viene
`false` y `motivosBloqueo` explica por qué.

### Para el frontend

1. Llamar `GET /datos-importantes/verificar?tipo=...&id=...` y mostrar la lista de `cascada`
   (lo que se va a perder) en un diálogo de confirmación del tipo "esto se va a perder para
   siempre, ¿continuar?".
2. Pedir que el usuario escriba/confirme la contraseña en ese mismo diálogo.
3. Llamar al `DELETE` correspondiente con `{ "password": "..." }` en el body.
4. Cada eliminación queda registrada en auditoría con quién la hizo.

---

## 6. Asociados: teléfono opcional y estado de cuenta

- **`telefonoPrincipal` ya no es obligatorio** al crear un asociado (`POST /asociados`). Se
  puede omitir o enviar `null`.
- La respuesta de un asociado (`AsociadoResponse`, en crear/editar/obtener/buscar/listar) ahora
  incluye un campo nuevo: **`tieneCuenta`** (`true`/`false`) — indica si ese asociado ya tiene
  una cuenta de usuario creada para iniciar sesión.

---

## 7. Multas independientes ("aparte")

Hasta ahora, toda multa se sumaba automáticamente a la siguiente factura del asociado. Ahora se
puede registrar una multa **independiente**, que nunca se junta con una factura: se paga por su
cuenta.

`POST /tesoreria/multas` — mismo endpoint de siempre, con un campo nuevo opcional:

```json
{
  "asociadoId": 0,
  "motivo": "...",
  "valor": 0,
  "independiente": true
}
```

- Si `independiente` es `true`, **no** se debe enviar `facturaId` (son conceptos que se
  excluyen entre sí).
- Una multa independiente nunca se incluye automáticamente en la próxima factura del asociado.

### Endpoints nuevos

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/tesoreria/multas` | Todas las multas del sistema, de todos los asociados (antes solo existía filtrado por asociado). |
| `PATCH` | `/tesoreria/multas/{id}/pagar` | Pago directo de una multa independiente. Falla si la multa no es independiente (esas se pagan junto con su factura) o si ya está pagada/anulada. |
| `PATCH` | `/tesoreria/multas/{id}` | Editar **solo el valor** de una multa (el motivo queda fijo desde su creación). Body: `{ "valor": 0 }`. Bloqueado si ya está pagada/anulada o si ya quedó incluida en una factura. |

Borrado definitivo de una multa: ver [sección 5](#5-módulo-nuevo-gestión-de-datos-importantes)
(`DELETE /datos-importantes/multas/{id}`).

---

## 8. Listado de cuentas de usuario

`POST /auth/usuarios/listar` — exclusivo del Administrador, body:

```json
{ "password": "..." }
```

Pide reconfirmar la contraseña del Administrador aunque ya tenga sesión iniciada, y devuelve
la lista completa de cuentas (usuario, correo, rol, activo/inactivo, último login, etc.).

### ⚠️ Sobre "mostrar la contraseña actual" — no es técnicamente posible

El pedido original incluía mostrar la contraseña actual de cada cuenta. Esto **no se
implementó**, y no es un tema de permisos ni de que falte una pantalla: las contraseñas se
guardan con un hash de un solo sentido (BCrypt). Eso significa que la contraseña original
**no existe en ningún lado del sistema después de crearse** — ni el backend, ni la base de
datos, ni el propio Administrador la pueden recuperar. Es lo mismo que pasa en cualquier sistema
bien construido (bancos, redes sociales, etc.): nadie del lado del servidor puede "ver" tu
contraseña, solo verificar si la que escribiste coincide.

Lo que sí se puede hacer si el objetivo real es ayudar a alguien que perdió su acceso:
restablecerle la contraseña (ya existe la creación de cuenta con una contraseña nueva, y el
propio usuario puede cambiarla desde `PUT /auth/cambiar-password`). Si quieren un endpoint
específico de "Administrador resetea la contraseña de cualquier cuenta sin saber la anterior",
es una función nueva concreta y chica — avisen si la quieren y se agrega.

---

## 9. Formularios: programación automática y manual

Antes de esta ronda, los campos `fechaInicio`/`fechaFin` de un formulario **existían pero no
hacían nada** — el formulario solo se activaba o desactivaba a mano (`POST
/encuestas/{id}/activar` y `.../desactivar`, que siguen funcionando exactamente igual).

### Qué cambia

- **Si el formulario tiene `fechaInicio`** (modo programado): se abre solo apenas llega esa
  fecha/hora, **sin** necesidad de que un administrador lo active a mano. Si ya está abierto a
  esa hora, no pasa nada (no lo "reabre" ni lo toca).
- **Si además tiene `fechaFin`**: se cierra solo apenas llega esa fecha/hora, de la misma forma.
- **Si no tiene ninguna de las dos fechas** (modo manual): sigue funcionando exactamente como
  hasta ahora, 100% a mano con activar/desactivar.
- **Validación nueva al crear**: si se manda `fechaInicio` y/o `fechaFin`, tienen que ser
  posteriores al momento actual (si no, el servidor rechaza la creación con un mensaje claro).
- Un administrador puede seguir activando/desactivando a mano un formulario **aunque tenga
  fechas programadas** — la programación no bloquea el control manual, solo actúa cuando el
  estado quedó "atrasado" respecto a lo programado.
- Nunca se reabre automáticamente un formulario que ya fue finalizado/archivado a mano; eso es
  siempre una decisión manual.

### Nota técnica (por qué es confiable aunque el servicio se duerma)

Igual que con el hero (ver sección 1): en vez de depender de una tarea de fondo que podría no
correr si Render apagó el servicio por inactividad, el sistema **recalcula el estado correcto
en el momento en que alguien consulta o usa ese formulario puntual**. Además hay un barrido
automático cada 5 minutos como mejora de experiencia mientras el servicio está despierto, pero
no es lo único que garantiza que funcione — es un plus, no el mecanismo principal.

### Corrección de un hueco de seguridad

Se encontró y corrigió algo que no estaba bien: un formulario con `requiereAutenticacion=true`
se podía **ver** sin haber iniciado sesión (solo bloqueaba responder, no consultar). Ahora
también bloquea la vista — aplica tanto a `GET /encuestas/{id}` como a `GET
/encuestas/codigo/{codigo}` (el que se usa al escanear el QR).

---

## 10. Formularios: configuración general al crear

Se organizó y en un caso se corrigió el comportamiento de las opciones generales al crear un
formulario (`POST /encuestas`):

| Opción | Comportamiento |
|---|---|
| `publico` | **Corregido**: cuando es `true` **y no se programó `fechaInicio`**, el formulario se publica de inmediato al crearse (antes esto no hacía nada, siempre quedaba en borrador). Si sí tiene `fechaInicio`, el arranque lo maneja la programación (sección 9), no esta opción. |
| `requiereAutenticacion` | Ya funcionaba para bloquear respuestas; ahora también bloquea la **vista** sin sesión iniciada (ver sección 9). |
| `respuestaUnica` | **Importante, matiz real, no es un "ya funciona" sin más**: el backend solo puede impedir una segunda respuesta cuando quien responde **tiene sesión iniciada** (lo identifica por su usuario). Si el formulario no requiere autenticación y alguien responde sin sesión, el backend **no tiene forma de saber quién es** — por eso, tal como ya lo describieron ustedes mismos, el "una sola vez" para respondientes anónimos depende de que **el frontend lo recuerde localmente** (ej. localStorage) y no vuelva a mostrar el formulario. Para usuarios con sesión, el backend sí lo bloquea de por sí, sin depender del frontend. |
| Opciones múltiples vs. única | Es por pregunta, no a nivel de formulario: al crear una pregunta de tipo opciones, `tipo` puede ser `OPCION_UNICA` o `OPCION_MULTIPLE`. **Se encontró y corrigió un hueco real**: el campo ya existía pero no se validaba nada al responder — se podían mandar varias respuestas a una pregunta de opción única sin que el backend lo rechazara. Ahora sí se valida: si la pregunta no es `OPCION_MULTIPLE`, mandar más de una respuesta para esa pregunta da error. Para el frontend: en una pregunta `OPCION_MULTIPLE`, cada opción elegida se manda como un item separado en `respuestas` (mismo `preguntaId`, distinto `valor`); en `OPCION_UNICA`, un solo item. |

---

## 11. Infraestructura: Render + Supabase

Se investigó el motivo de la demora al arrancar. Resumen honesto: **una parte importante es
inherente al plan gratuito de Render** y ningún cambio de código la elimina del todo — el
servicio se apaga tras ~15 min sin tráfico y tarda entre 30 y 60 segundos en volver a arrancar
cuando llega la primera petición. Eso solo se elimina con un plan pago (siempre activo).

Lo que sí se hizo, dentro de lo que el código puede controlar:

- **Banderas de la JVM** en el `Dockerfile` (`-XX:TieredStopAtLevel=1 -XX:+UseSerialGC`):
  reducen el tiempo que la aplicación tarda en arrancar *dentro* de esa ventana de spin-up,
  apropiado para un contenedor tan chico (512MB RAM / 0.1 CPU en el plan gratuito). No cambian
  el comportamiento de la aplicación, solo cómo la JVM usa CPU/memoria al iniciar.
- **Cacheo de dependencias Maven** en el build del Dockerfile: acelera el tiempo de
  *despliegue* (cuando hacen `git push` y Render reconstruye la imagen), no el arranque en frío
  del contenedor ya construido — son dos cosas distintas, ambas mejoradas.

### Algo para que verifiquen ustedes (no lo pude ver yo)

Si la variable `DATABASE_URL` / `DB_URL` configurada en Render apunta a la conexión **directa**
de Supabase (`db.<proyecto>.supabase.co:5432`), esa dirección es IPv6 por defecto, y **Supabase
señala explícitamente a Render como una plataforma que no soporta IPv6 saliente** — esto puede
sumar demora extra (o directamente fallar la conexión) en cada arranque en frío. Si es el caso,
cambiar a la cadena de conexión del **Session Pooler** de Supabase (mismo puerto 5432, otro
host — se copia desde el dashboard de Supabase, sección "Connect") es la corrección oficialmente
recomendada para esta combinación exacta Render+Supabase, y no cambia el comportamiento de la
aplicación. Es un valor que se configura en el dashboard de Render, así que no lo pude revisar
ni cambiar yo directamente.

---

## 12. Portal Público: eliminación definitiva de etiquetas

Dentro del Portal Público (módulo 11/12: galería, publicaciones destacadas, categorías, etiquetas
y videos) ya se podían crear y listar etiquetas, pero **no eliminarlas**. Ahora se agrega el
borrado **definitivo**:

`DELETE /publicaciones/etiquetas/{id}` — Admin o Tesorero.

- Borra la etiqueta de inmediato y para siempre.
- Antes de borrarla, la **desvincula de todas las publicaciones** que la usan (limpia la tabla
  intermedia), así ninguna publicación queda apuntando a una etiqueta inexistente.
- Cada eliminación queda registrada en auditoría (`ELIMINAR_ETIQUETA`).

### Para el frontend

En la pantalla de administración de etiquetas, agregar un botón de eliminar por etiqueta, con la
confirmación habitual antes de llamar al endpoint (borrado permanente e irreversible).

---

## 13. Corrección: 500 en configuracion/hero por columnas faltantes

**Síntoma:** algunas funciones devolvían `500` con "Ocurrió un error inesperado" — en concreto
`GET /configuracion`, `GET /configuracion/hero/actual` y las consultas sobre multas. En los logs
de Render aparecía:

```
ERROR: column c1_0.modo_hero does not exist
SQLState: 42703
```

**Causa:** el proyecto usa `spring.jpa.hibernate.ddl-auto=update`, que **no puede agregar una
columna `NOT NULL` a una tabla que ya tiene filas** (la base la rechaza sin un valor por defecto).
Por eso columnas agregadas en rondas posteriores (`configuracion.modo_hero`,
`configuracion.hero_rotacion_actual_id`, `configuracion.hero_rotacion_desde`,
`configuracion.auditoria_activa`, `multa.independiente`) quedaron ausentes en la base de
producción, y cualquier `SELECT` que las incluyera fallaba con 500.

**Solución (no requiere tocar la base a mano):** se agregó `ConfiguracionSchemaMigrator`, un
componente que corre en cada arranque **después** de que Hibernate actualiza el esquema y hace lo
siguiente de forma idempotente y segura (funciona en H2 dev y PostgreSQL prod):

1. Agrega las columnas faltantes con `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`.
2. Rellena las filas existentes con el valor por defecto de la entidad (ej. `modo_hero='UNICO'`,
   `auditoria_activa=TRUE`, `independiente=FALSE`).
3. Aplica `NOT NULL` solo a las columnas que la entidad declara como tal.

Al desplegar esta versión, el backend repara solo el esquema en el primer arranque y las funciones
vuelven a responder normalmente. No hay que ejecutar nada manual.

---

## 14. Tabla resumen de endpoints nuevos/modificados

| Método | Ruta | Novedad |
|---|---|---|
| `GET` | `/configuracion/hero/actual` | Nuevo — público |
| `POST` | `/configuracion/hero` | Nuevo |
| `GET` | `/configuracion/hero` | Nuevo |
| `DELETE` | `/configuracion/hero/{id}` | Nuevo |
| `PATCH` | `/configuracion/hero/{id}/principal` | Nuevo |
| `PUT` | `/configuracion/hero/modo` | Nuevo |
| `POST` | `/lecturas` | Modificado — `lecturaAnterior` opcional |
| `GET` | `/lecturas/anterior-por-defecto` | Nuevo — lectura anterior que se usará por defecto (antes de registrar) |
| `PUT` | `/lecturas/{id}` | Modificado (ver nota: `lecturaAnterior` se ignora) |
| `POST` | `/reportes` | Modificado — `imagenUrl` opcional |
| `DELETE` | `/reportes/{id}` | Nuevo |
| `DELETE` | `/datos-importantes/formularios/{id}` | Nuevo — borrado definitivo en cascada |
| `DELETE` | `/datos-importantes/facturas/{id}` | Nuevo — cascada de pagos/recibos/movimientos |
| `DELETE` | `/datos-importantes/recibos/{id}` | Nuevo |
| `DELETE` | `/datos-importantes/asociados/{id}` | Nuevo — cascada total del historial, medidor solo se desvincula |
| `DELETE` | `/datos-importantes/cuentas/{id}` | Nuevo — cascada de dependencias |
| `DELETE` | `/datos-importantes/periodos-contables/{id}` | Nuevo — cascada del mes |
| `DELETE` | `/datos-importantes/anios-contables/{id}` | Nuevo — cascada de meses |
| `DELETE` | `/datos-importantes/multas/{id}` | Nuevo — descuenta el valor de la factura si estaba incluida |
| `DELETE` | `/datos-importantes/medidores/{id}` | Nuevo — desvincula al asociado y borra lecturas en cascada |
| `DELETE` | `/datos-importantes/lecturas/{id}` | Nuevo — cascada de la factura si la generó |
| `DELETE` | `/datos-importantes/pagos/{id}` | Nuevo — reversa contable (recibo + movimientos + recalculo de factura) |
| `DELETE` | `/datos-importantes/movimientos/{id}` | Nuevo |
| `DELETE` | `/datos-importantes/notificaciones/{id}` | Nuevo |
| `GET` | `/datos-importantes/verificar` | Nuevo — diagnóstico previo de qué se va a borrar en cascada |
| `POST` | `/asociados` | Modificado — `telefonoPrincipal` ya no obligatorio |
| `*` (varios) | Respuestas de asociado | Modificado — nuevo campo `tieneCuenta` |
| `POST` | `/tesoreria/multas` | Modificado — campo `independiente` opcional |
| `GET` | `/tesoreria/multas` | Nuevo |
| `PATCH` | `/tesoreria/multas/{id}/pagar` | Nuevo |
| `PATCH` | `/tesoreria/multas/{id}` | Nuevo |
| `POST` | `/auth/usuarios/listar` | Nuevo |
| `POST` | `/encuestas` | Modificado — validación de fechas + `publico` activa de inmediato |
| `GET` | `/encuestas/{id}` | Modificado — bloquea vista sin sesión si `requiereAutenticacion` |
| `GET` | `/encuestas/codigo/{codigo}` | Modificado — idem |
| `POST` | `/encuestas/{id}/responder` | Modificado — ahora valida opción única/múltiple (antes no se validaba nada) |
| `DELETE` | `/publicaciones/etiquetas/{id}` | Nuevo — eliminación definitiva de etiqueta |
| `GET` | `/informes/listado-asociados` | Nuevo — datos del listado general de asociados |
| `GET` | `/informes/listado-asociados/html` | Nuevo — vista previa en HTML |
| `GET` | `/informes/listado-asociados/pdf` | Nuevo — descarga en PDF (logo, sin firma ni sello) |
| `POST` | `/facturas/{id}/public-link` | Nuevo — enlace público temporal de la factura (Admin o Tesorero) |
| `POST` | `/recibos/{numeroRecibo}/public-link` | Nuevo — enlace público temporal del recibo (Admin o Tesorero) |
| `GET` | `/public/facturas/{token}` | Nuevo — descarga pública del PDF de la factura, sin login |
| `GET` | `/public/recibos/{token}` | Nuevo — descarga pública del PDF del recibo, sin login |
| `PATCH` | `/auth/usuarios/{id}/estado` | Nuevo — activar o bloquear cuenta (solo Admin) |

Todo lo demás (Swagger UI incluido) se actualiza solo a partir de las anotaciones en el código —
no hace falta ningún paso manual adicional para que aparezca documentado ahí.

---

## 15. Listado general de asociados (HTML/PDF)

Nuevo módulo dentro de **Informes** para descargar el **listado completo de asociados** en un solo
documento, exactamente con el mismo enfoque que los informes, facturas y recibos: misma plantilla
Thymeleaf para la **vista previa en HTML** y para la **descarga en PDF** (misma información, mismo
diseño), con el **logo institucional** pero **sin firma ni sello** (a diferencia de los informes
de gestión).

### Comportamiento

- Incluye **todos** los asociados **no archivados** (los archivados **no** aparecen, ni en la vista
  previa ni en el PDF).
- Se ordenan por su **id interno** (equivalente al `codigoInterno`).
- De cada asociado se muestra el **detalle completo**: código interno, nombre completo, documento,
  teléfono, correo, dirección, **estado del servicio**, **número y estado del medidor**, y un
  indicador de **si tiene cuenta en el sistema** (`SI` / `NO`).
- Arriba hay un resumen con totales: total de asociados, activos, suspendidos, **con cuenta** y
  **sin cuenta**.

### Endpoints

| Método | Ruta | Quién | Descripción |
|---|---|---|---|
| `GET` | `/informes/listado-asociados` | Admin o Tesorero | Datos del listado en JSON (útil para previsualizar en la UI antes de descargar). |
| `GET` | `/informes/listado-asociados/html` | Admin o Tesorero | Vista previa del documento en HTML (idéntica al PDF). |
| `GET` | `/informes/listado-asociados/pdf` | Admin o Tesorero | Descarga el listado en PDF. `Content-Disposition: attachment`. |

### Para el frontend

- En la pantalla de Informes agregar un acceso "Listado de Asociados" (Admin o Tesorero).
- Para previsualizar: abrir/incrustar `GET /informes/listado-asociados/html` (respuesta `text/html`).
- Para descargar: `GET /informes/listado-asociados/pdf` con `responseType: 'blob'` y generar el link
  de descarga con el nombre `listado-asociados-<fecha>.pdf`.
- Si se quiere mostrar el resumen con los totales como tarjetas antes de descargar, usar
  `GET /informes/listado-asociados` (JSON) con los campos `totalAsociados`, `asociadosActivos`,
  `asociadosSuspendidos`, `asociadosConCuenta` y `asociadosSinCuenta`.

---

## 16. Chat interno (módulo nuevo)

Nuevo módulo de **mensajería interna** entre `ASOCIADO`, `TESORERO` y `ADMINISTRADOR`.
Solo **texto y emojis** (no admite imágenes, archivos, audios ni videos). La base del módulo
es `/api/chat` (diferente de `/api/v1`).

### Reglas de negocio implementadas

- **Roles permitidos**: Asociado, Tesorero y Administrador. El usuario público no tiene acceso
  a ninguna ruta de chat.
- **Asociado ↔ Asociado**: no se permite crear esa conversación (solo habla con Admin o Tesorero).
- **Sin conversaciones duplicadas**: si la pareja ya tiene una conversación, `POST
  /api/chat/conversaciones` devuelve la existente.
- **El remitente siempre sale del JWT**; el backend nunca confía en un `usuarioId` del body.
- **Sin eliminación directa de mensajes**: no existe un `DELETE` de mensajes. El flujo es:
  `Solicitud (PENDIENTE)` → el otro participante **acepta** (se borra definitivamente) o
  **rechaza** (se conserva). El solicitante puede **cancelar** su propia solicitud pendiente.
- **Eliminación de conversación completa**: solo Admin/Tesorero, definitiva y transaccional
  (borra conversación + mensajes + solicitudes relacionadas). El asociado no puede.
- **Retención de 8 días**: una tarea programada (00:30 diaria) elimina en PostgreSQL los
  mensajes con más de 8 días junto con sus solicitudes asociadas. El historial completo vive
  en **IndexedDB** del lado del frontend; el backend solo sirve como buffer temporal.

### Endpoints

| Método | Ruta | Quién | Descripción |
|---|---|---|---|
| `POST` | `/api/chat/conversaciones` | Asociado, Tesorero, Admin | Crear u obtener la conversación con un `destinatarioId`. |
| `GET` | `/api/chat/conversaciones` | Asociado, Tesorero, Admin | Lista de conversaciones del usuario: participante, último mensaje, no leídos, solicitudes pendientes. |
| `GET` | `/api/chat/conversaciones/{id}/mensajes` | Asociado, Tesorero, Admin | Mensajes disponibles (opcional `?desde={id}` o `?desdeFecha={ISO}` para sincronizar con IndexedDB). |
| `POST` | `/api/chat/conversaciones/{id}/mensajes` | Asociado, Tesorero, Admin | Enviar mensaje (texto/emojis, máx. 2000 caracteres). |
| `PATCH` | `/api/chat/mensajes/{id}` | Asociado, Tesorero, Admin | Editar mensaje propio (marca `editado`). |
| `POST` | `/api/chat/mensajes/{mensajeId}/solicitud-eliminacion` | Asociado, Tesorero, Admin | Solicitar eliminar un mensaje propio (queda `PENDIENTE`). |
| `GET` | `/api/chat/solicitudes-eliminacion` | Asociado, Tesorero, Admin | Solicitudes pendientes del usuario (a confirmar y enviadas). |
| `PATCH` | `/api/chat/solicitudes-eliminacion/{id}/aceptar` | Asociado, Tesorero, Admin | Aceptar: elimina el mensaje definitivamente (`mensajeEliminado: true`). |
| `PATCH` | `/api/chat/solicitudes-eliminacion/{id}/rechazar` | Asociado, Tesorero, Admin | Rechazar: conserva el mensaje. |
| `PATCH` | `/api/chat/solicitudes-eliminacion/{id}/cancelar` | Asociado, Tesorero, Admin | Cancelar la propia solicitud (`CANCELADA`). |
| `PATCH` | `/api/chat/conversaciones/{id}/leidos` | Asociado, Tesorero, Admin | Marcar como leídos los mensajes recibidos en la conversación. |
| `DELETE` | `/api/chat/conversaciones/{id}` | **Solo** Admin o Tesorero | Eliminar conversación completa (definitivo). |

### Para el frontend

- **Base URL**: `/api/chat` con el mismo Bearer token de `/api/v1/auth/login`.
- **Persistencia local**: guardar el historial en **IndexedDB**. Al abrir una conversación,
  llamar `GET .../mensajes` (con `?desde=` si ya hay datos locales) y aplicar al UI los que
  aún no tenga. No depender de que el backend conserve mensajes: a los 8 días desaparecen.
- **Estado de lectura**: `PATCH /api/chat/conversaciones/{id}/leidos` al entrar/salir de una
  conversación. El campo `leido` de cada `MensajeResponse` indica si el **destinatario** ya lo
  leyó.
- **Eliminación de mensajes** (flujo de confirmación mutua):
  1. El autor llama `POST /mensajes/{id}/solicitud-eliminacion` → queda `PENDIENTE`.
  2. El otro participante la ve en `GET /solicitudes-eliminacion` y responde:
     - `PATCH .../aceptar` → la respuesta trae `mensajeEliminado: true`; el frontend debe
       **borrar ese mensaje también de IndexedDB** y propagar el cambio al otro cliente.
     - `PATCH .../rechazar` → el mensaje sigue visible.
  3. El solicitante puede `PATCH .../cancelar` mientras esté `PENDIENTE`.
- **Indicadores en la lista de conversaciones**: `solicitudPendienteEnviada` y
  `solicitudPendienteRecibida` permiten mostrar el estado de la solicitud en la UI.
- **Aceptar/rechazar genera una notificación** para el solicitante (módulo de Notificaciones).

> Nota: las tablas del chat (`conversaciones`, `mensajes`, `solicitudes_eliminacion`) se crean
> automáticamente con `ddl-auto: update`, igual que el resto del esquema.

---

## 17. Enlace público de descarga de facturas y recibos

Nueva funcionalidad para **enviar por WhatsApp** la factura o el recibo en PDF sin que la persona
tenga que iniciar sesión. Un Administrador o Tesorero genera un **enlace temporal** desde el
backend, y ese enlace permite a cualquiera **descargar el PDF directamente** (solo lectura, sin
login).

### Cómo funciona

| Método | Ruta | Quién | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/facturas/{id}/public-link` | **Admin o Tesorero** | Genera (o renueva) el enlace público de esa factura. |
| `POST` | `/api/v1/recibos/{numeroRecibo}/public-link` | **Admin o Tesorero** | Genera (o renueva) el enlace público de ese recibo. |
| `GET` | `/api/v1/public/facturas/{token}` | **Público** (sin login) | Entrega el PDF de la factura listo para descargar. |
| `GET` | `/api/v1/public/recibos/{token}` | **Público** (sin login) | Entrega el PDF del recibo listo para descargar. |

> Para el recibo se usa `numeroRecibo` (igual que los endpoints existentes `.../pdf` y
> `.../html`). Para la factura se usa el `id` numérico (igual que `.../pdf` y `.../html`).

### Reglas implementadas

- **Duración**: el enlace dura **72 horas** por defecto (configurable con la variable de entorno
  `PUBLIC_LINK_EXPIRATION_HOURS`). Pasado ese tiempo el enlace **se borra definitivamente**.
- **Un solo enlace activo por documento**: generar un enlace nuevo elimina el anterior del mismo
  documento (no se acumulan enlaces viejos).
- **El enlace público NO requiere iniciar sesión**: valida que el token exista, que no esté
  vencido, que la factura/recibo exista y que **no esté anulado**. Si ya venció o no existe,
  responde `404` con el mensaje **"Este enlace dejó de estar disponible."**
- **Tarea automática**: cada día a las 00:40 el backend borra todos los enlaces vencidos
  (además de borrarse al intentar usarlos si ya vencieron).

### Ejemplo de la respuesta al generar el enlace

```json
{
  "documentoId": 123,
  "numeroDocumento": "F-00001",
  "tipo": "FACTURA",
  "publicDownloadUrl": "https://acueducto-losguaduales-server.onrender.com/api/v1/public/facturas/aB3xYz...",
  "expiresAt": "2026-08-22T23:59:59"
}
```

Para un recibo, `tipo` será `"RECIBO"`, `numeroDocumento` el número del recibo y
`publicDownloadUrl` apuntará a `/api/v1/public/recibos/{token}`.

### Nota técnica (por qué el enlace funciona aunque el servicio se duerma)

Al igual que el hero y los formularios programados (secciones 1 y 9): la **vigencia se calcula en
el momento de la descarga**, no depende de una tarea de fondo. Si Render apagó el servicio por
inactividad, al abrir el enlace el backend arranca, compara `fechaExpiracion` con la hora actual
y responde el PDF (o el mensaje de enlace vencido si ya pasó el plazo).

### Para el frontend

1. **Generar el enlace** (pantalla de Tesorería/Administración): en el detalle de una factura o
   de un recibo, agregar un botón **"Compartir / Enviar por WhatsApp"** que llame a
   `POST /api/v1/facturas/{id}/public-link` o `POST /api/v1/recibos/{numeroRecibo}/public-link`
   con el Bearer token de Admin/Tesorero.
2. **Enviarlo**: de la respuesta tomar `publicDownloadUrl` y abrir
   `https://wa.me/?text=<publicDownloadUrl>` (o copiar el enlace para pegarlo donde quieran).
3. **Vencimiento**: el campo `expiresAt` permite mostrar al lado del botón cuándo deja de servir
   el enlace (por ejemplo "Vence: 22/08/2026").
4. **Al abrir el enlace** (quien lo recibe): el navegador descarga el PDF automáticamente
   (`Content-Disposition: attachment`). Si el enlace está vencido o no existe, el backend responde
   un JSON de error `404` con el mensaje **"Este enlace dejó de estar disponible."** — si se quiere
   mostrar una pantalla amigable en el frontend, puede usarse el mensaje del campo `mensaje` de la
   respuesta de error estándar.

> La tabla nueva (`enlaces_publicos_documentos`) se crea automáticamente con `ddl-auto: update`,
> igual que el resto del esquema. No hace falta ejecutar nada manual en Supabase.

---

## 18. Corrección: Swagger "Failed to fetch" en Render

**Síntoma:** Swagger funcionaba en local, pero en el servidor desplegado
(`https://acueducto-losguaduales-server.onrender.com/swagger-ui/index.html`) al hacer clic en
"Execute"/"Try it out" salía:

```
Failed to fetch.
Possible Reasons:
* CORS
* Network Failure
* URL scheme must be "http" or "https" for CORS request.
```

**Causa:** `OpenApiConfig` declaraba dos servidores fijos en el documento OpenAPI:
`http://localhost:8080` (primero) y la URL de Render. Swagger UI usa **por defecto el primero**,
así que desde el navegador del usuario los intentos de prueba se enviaban a `localhost:8080` de su
propia máquina (o con un esquema incorrecto), y fallaban con "Failed to fetch". El frontend seguía
funcionando porque no depende de esa lista de servidores.

**Solución:** se reemplazaron los servidores fijos por un único servidor relativo:

```java
@Server(url = "/", description = "Servidor actual: usa el mismo origen desde donde se abre Swagger")
```

Con esto Swagger envía las llamadas de prueba **al mismo origen desde donde se abrió la UI**
(local o Render), sin importar el esquema (http/https) ni el host. También se agregó
`server.forward-headers-strategy: framework` para que Spring respete los headers `X-Forwarded-*`
que envía el proxy de Render.

**Para el frontend:** ningún cambio. Los endpoints siguen igual; esto solo afecta a la UI de
Swagger en el servidor desplegado.

---

## 19. Bloqueo de cuentas de usuario

### Descripción general

Un Administrador puede **bloquear o activar** cualquier cuenta de usuario del sistema (excepto la
cuenta del administrador principal, id=1, que nunca puede ser bloqueada). La afectación aplica a
**todas las cuentas sin importar el rol** (admin, tesorero o asociado).

### Comportamiento

- **Activo**: el usuario usa el sistema normalmente (login, servicios, todo funciona).
- **Bloqueado**:
  - **No puede iniciar sesión**: al intentar hacer login, recibe un error con el motivo del bloqueo.
  - **No puede usar ningún servicio**: si ya tenía sesión activa cuando fue bloqueado, el JWT
    filter lo rechaza automáticamente con un 403 y el motivo del bloqueo.
  - **El refresh token también se rechaza**: no puede renovar su token de acceso.

### Endpoint nuevo: `PATCH /api/v1/auth/usuarios/{id}/estado`

Exclusivo del **Administrador**. Activa o bloquea una cuenta.

**Body:**

```json
{
  "password": "contraseña_del_admin_logueado",
  "motivo": "Mal uso de la plataforma"
}
```

| Campo | Requerido | Descripción |
|---|---|---|
| `password` | Sí | Contraseña del administrador autenticado (reconfirmación de seguridad). |
| `motivo` | No | Texto libre que se mostrará al usuario bloqueado. Si se omite o es null, se guarda vacío. |

**Reglas:**

- El administrador **no puede bloquear su propia cuenta** (devuelve error).
- La cuenta del **administrador principal (id=1)** nunca puede ser bloqueada (devuelve error).
- Solo usuarios con rol `ADMINISTRADOR` pueden usar este endpoint.
- La contraseña se verifica aunque el admin ya tenga sesión iniciada.

**Ejemplo de respuesta (éxito):**

```json
{
  "id": 5,
  "username": "tesorero1",
  "email": "tesorero@acueducto.com",
  "rol": "TESORERO",
  "activo": false,
  "asociadoId": null
}
```

### Mensaje que recibe el usuario bloqueado

**Al intentar iniciar sesión** (login o refresh), el backend retorna un error 422 con el
siguiente formato:

```json
{
  "mensaje": "Lo sentimos, tu cuenta ha sido bloqueada por uno de nuestros administradores. Asunto: Mal uso de la plataforma"
}
```

**Al tener sesión activa y hacer cualquier petición** ( JWT filter), el backend retorna un 403:

```json
{
  "mensaje": "Lo sentimos, tu cuenta ha sido bloqueada por uno de nuestros administradores. Asunto: Mal uso de la plataforma",
  "motivoBloqueo": "Mal uso de la plataforma"
}
```

### Campo nuevo en la entidad `usuarios`

| Columna | Tipo | Descripción |
|---|---|---|
| `motivo_bloqueo` | VARCHAR(500), nullable | Almacena el motivo del bloqueo. Se limpia al desbloquear. |

La tabla se actualiza automáticamente con `ddl-auto: update` ( Hibernate agrega la columna en el
primer arranque).

### Para el frontend

1. **Listado de cuentas** (`POST /auth/usuarios/listar`): ya muestra el campo `activo` de cada
   cuenta. Puede usarlo para mostrar un indicador visual de estado (punto verde/rojo).
2. **Botón de bloquear/desbloquear**: al lado de cada cuenta en el listado, agregar un botón que
   abra un diálogo pidiendo:
   - La contraseña del administrador (reconfirmación).
   - Un motivo (obligatorio al bloquear, opcional al desbloquear).
3. **Al recibir un 422 o 403** con `motivoBloqueo`: mostrar el mensaje al usuario en pantalla
   (por ejemplo un modal o pantalla de "Tu cuenta ha sido bloqueada") con el motivo si existe.
4. **Cerrar sesión automáticamente**: cuando el frontend reciba un 403 del JWT filter (usuario
   bloqueado con sesión activa), debe limpiar el token del almacenamiento local y redirigir al
   login con el mensaje del bloqueo.
