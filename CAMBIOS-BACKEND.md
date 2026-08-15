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
13. [Tabla resumen de endpoints nuevos/modificados](#13-tabla-resumen-de-endpoints-nuevosmodificados)

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
irreversible** de: formularios, facturas, recibos, asociados, cuentas y periodos contables (más
multas, ver punto 7). Base: `/api/v1/datos-importantes`.

### Cómo funciona cada endpoint

Todos son `DELETE /datos-importantes/{tipo}/{id}`, y **todos piden la contraseña del
Administrador en el body**, aunque ya tenga sesión iniciada:

```json
{ "password": "..." }
```

| Tipo | Ruta | Bloqueado cuando... |
|---|---|---|
| Formulario | `DELETE /datos-importantes/formularios/{id}` | Nunca — borra en cascada sus preguntas y respuestas ya recibidas. |
| Factura | `DELETE /datos-importantes/facturas/{id}` | Tiene pagos registrados (se perdería dinero real ya recibido). |
| Recibo | `DELETE /datos-importantes/recibos/{id}` | Nunca — pero no revierte el estado de la factura/pago asociados. |
| Asociado | `DELETE /datos-importantes/asociados/{id}` | Tiene historial (facturas, pagos o lecturas) — en ese caso, usar "archivar" en su lugar. También bloqueado si tiene una cuenta de usuario vinculada (hay que borrar la cuenta primero, por separado). |
| Cuenta | `DELETE /datos-importantes/cuentas/{id}` | Es la propia cuenta del Administrador que está haciendo la petición, o es autor de algún formulario. |
| Periodo contable | `DELETE /datos-importantes/periodos-contables/{id}` | Tiene lecturas o facturas registradas en ese mes. |
| Multa | `DELETE /datos-importantes/multas/{id}` | Ya quedó incluida en una factura. |

Cuando algo está bloqueado, el servidor responde con un mensaje explicando por qué y, cuando
aplica, qué hacer en su lugar (por ejemplo "archive al asociado en vez de eliminarlo").

### ⚠️ Nota importante — esto no es literal a lo pedido, léanla

El pedido original decía "eliminar definitivamente" sin más condiciones. Se implementó **con
las validaciones de la tabla de arriba**, en vez de un borrado incondicional, por una razón
concreta: el propio proyecto ya tiene una regla explícita para asociados ("un asociado con
historial nunca se elimina físicamente") precisamente porque borrar facturas/pagos sin cuidado
rompe la integridad de la contabilidad — un borrado sin ningún control podría, por ejemplo,
fallar a mitad de camino por una restricción de la base de datos, o peor, "funcionar" pero dejar
huecos en reportes ya generados. Se aplicó el mismo criterio de protección a los demás tipos.
Si en algún caso puntual necesitan saltarse una de estas validaciones, avisen y se ajusta ese
caso específico.

### Para el frontend

Antes de llamar a cualquiera de estos endpoints, mostrar una confirmación explícita del tipo
"esto se va a perder para siempre, ¿continuar?" y pedir que el usuario escriba/confirme la
contraseña en ese mismo diálogo (tal como se pidió). Cada eliminación queda registrada en
auditoría con quién la hizo.

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

## 13. Tabla resumen de endpoints nuevos/modificados

| Método | Ruta | Novedad |
|---|---|---|
| `GET` | `/configuracion/hero/actual` | Nuevo — público |
| `POST` | `/configuracion/hero` | Nuevo |
| `GET` | `/configuracion/hero` | Nuevo |
| `DELETE` | `/configuracion/hero/{id}` | Nuevo |
| `PATCH` | `/configuracion/hero/{id}/principal` | Nuevo |
| `PUT` | `/configuracion/hero/modo` | Nuevo |
| `POST` | `/lecturas` | Modificado — `lecturaAnterior` opcional |
| `PUT` | `/lecturas/{id}` | Modificado (ver nota: `lecturaAnterior` se ignora) |
| `POST` | `/reportes` | Modificado — `imagenUrl` opcional |
| `DELETE` | `/reportes/{id}` | Nuevo |
| `DELETE` | `/datos-importantes/formularios/{id}` | Nuevo |
| `DELETE` | `/datos-importantes/facturas/{id}` | Nuevo |
| `DELETE` | `/datos-importantes/recibos/{id}` | Nuevo |
| `DELETE` | `/datos-importantes/asociados/{id}` | Nuevo |
| `DELETE` | `/datos-importantes/cuentas/{id}` | Nuevo |
| `DELETE` | `/datos-importantes/periodos-contables/{id}` | Nuevo |
| `DELETE` | `/datos-importantes/multas/{id}` | Nuevo |
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

Todo lo demás (Swagger UI incluido) se actualiza solo a partir de las anotaciones en el código —
no hace falta ningún paso manual adicional para que aparezca documentado ahí.
