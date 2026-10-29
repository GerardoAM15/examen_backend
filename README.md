# AutoAndes API — Alarcon

Backend de las preguntas 1, 2 y 3 del examen: Spring Boot 4.0.7, Java 21, Oracle, JPA, Flyway, JWT y Swagger.
Proyecto y repositorio Git independientes. El frontend Angular se desarrolla por separado.

## Ejecutar

Requisitos: JDK 21, Oracle XE disponible en `localhost:1521/xepdb1` y el usuario/esquema `AUTODB` con permisos para crear tablas, indices y secuencias y cuota en su tablespace.

Desde esta carpeta, en PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

En Linux/macOS: `./mvnw spring-boot:run`.

- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs

El perfil predeterminado es `dev`. La conexion esta en `src/main/resources/application-dev.yaml`:
usuario `AUTODB`, contraseña local `1234567`. Flyway ejecuta `V1__esquema_y_semilla.sql` una sola vez.
Hibernate valida las tablas; no las crea ni las modifica. No ejecutes manualmente el script antes de arrancar.

Puedes cambiar la conexion sin editar archivos:

```powershell
$env:DB_URL = 'jdbc:oracle:thin:@//localhost:1521/xepdb1'
$env:DB_USERNAME = 'AUTODB'
$env:DB_PASSWORD = '1234567'
.\mvnw.cmd spring-boot:run
```

Si el puerto 8080 esta ocupado, detén la instancia anterior de AutoAndes.
Para generar el ejecutable: `.\mvnw.cmd clean package`.
Para ejecutarlo: `java -jar target/autoandes-backend-1.0.0.jar`.

## Usuarios de prueba

Cada usuario usa su nombre seguido de 123 (por ejemplo, admin123). La contraseña de Oracle es distinta de la contraseña del login.
Los hashes BCrypt estan en las migraciones V1 y V2; nunca se devuelven en el API.

| Usuario | Contraseña | Rol | Estado |
| --- | --- | --- | --- |
| admin | admin123 | ADMIN | Activo |
| asesor | asesor123 | ASESOR | Activo |
| jefe | jefe123 | JEFE_TALLER | Activo |
| inactivo | inactivo123 | ASESOR | Inactivo; login responde 401 |

Login:

```json
{ "username": "asesor", "password": "asesor123" }
```

`POST /api/v1/auth/login` devuelve:

```json
{
  "token": "<JWT>",
  "tipo": "Bearer",
  "expiraEnSegundos": 3600,
  "username": "asesor",
  "nombreCompleto": "Asesor AutoAndes",
  "roles": ["ASESOR"],
  "permisos": ["ORDEN_LEER", "ORDEN_REGISTRAR", "SERVICIO_LEER", "VEHICULO_ESCRIBIR", "VEHICULO_LEER"]
}
```

En Swagger ejecuta el login, copia `token`, pulsa **Authorize** y pega el token.
En el cliente envia `Authorization: Bearer <token>` en cada peticion.
`GET /api/v1/auth/me` devuelve id, username, nombreCompleto, roles y permisos.

Usuario inexistente, contraseña incorrecta y cuenta inactiva producen el mismo mensaje 401.
La API no usa sesiones del servidor. Cada peticion valida firma y vencimiento y vuelve a consultar el usuario y sus permisos.
Sin token o con token alterado/vencido: 401. Token valido sin permiso: 403.

## Permisos

| Permiso | ADMIN | ASESOR | JEFE_TALLER |
| --- | :---: | :---: | :---: |
| SERVICIO_LEER | Si | Si | Si |
| SERVICIO_ESCRIBIR | Si | — | Si |
| VEHICULO_LEER | Si | Si | Si |
| VEHICULO_ESCRIBIR | Si | Si | — |
| ORDEN_LEER | Si | Si | Si |
| ORDEN_REGISTRAR | Si | Si | — |
| ORDEN_ANULAR | Si | — | Si |
| REPORTE_VER | Si | — | Si |

`security/PermisosPorRol` centraliza la matriz y calcula la union para usuarios con varios roles.
`config/SecurityConfig` protege las rutas mediante `hasAuthority`.

## Contrato del API

Todas las rutas de la tabla llevan el prefijo `/api/v1`.
Solo el login, Swagger y las solicitudes OPTIONS son publicos.
Los listados devuelven arrays JSON, sin paginacion; una consulta sin resultados devuelve `[]`.

| Metodo | Ruta | Permiso | Respuesta correcta |
| --- | --- | --- | --- |
| POST | /auth/login | Publico | 200 |
| GET | /auth/me | Autenticado | 200 |
| GET | /clientes | VEHICULO_LEER | 200 |
| GET | /servicios | SERVICIO_LEER | 200 |
| GET | /servicios/{id} | SERVICIO_LEER | 200 |
| POST | /servicios | SERVICIO_ESCRIBIR | 201 |
| PUT | /servicios/{id} | SERVICIO_ESCRIBIR | 200 |
| DELETE | /servicios/{id} | SERVICIO_ESCRIBIR | 204 |
| PATCH | /servicios/{id}/estado | SERVICIO_ESCRIBIR | 200 |
| GET | /vehiculos?clienteId | VEHICULO_LEER | 200 |
| GET | /vehiculos/{id} | VEHICULO_LEER | 200 |
| POST | /vehiculos | VEHICULO_ESCRIBIR | 201 |
| PUT | /vehiculos/{id} | VEHICULO_ESCRIBIR | 200 |
| DELETE | /vehiculos/{id} | VEHICULO_ESCRIBIR | 204 |
| POST | /ordenes | ORDEN_REGISTRAR | 201 |
| GET | /ordenes?placa&estado&desde&hasta | ORDEN_LEER | 200 |
| GET | /ordenes/{id} | ORDEN_LEER | 200 |
| PATCH | /ordenes/{id}/anular | ORDEN_ANULAR | 200 |
| GET | /reportes/ingresos-por-servicio?desde&hasta | REPORTE_VER | 200 |

Errores: 400 para validacion o datos invalidos, 401 para autenticacion, 403 para permisos,
404 para recursos inexistentes, 409 para conflictos/reglas y 500 para fallos inesperados.

### Servicios

POST y PUT reciben:

```json
{ "codigo": "SRV-009", "nombre": "Cambio de bujias", "precio": 180.50, "duracionMin": 60, "activo": true }
```

La respuesta agrega `id`. Codigo: `SRV-000`, nombre: 3–80 caracteres, precio positivo con hasta dos decimales,
duracion: 15–480 minutos. Un codigo repetido produce 409.
DELETE borra solo si no tiene detalles de orden; si esta usado responde 409.
Para baja logica: PATCH `/servicios/{id}/estado` con `{ "activo": false }`.

### Vehiculos y clientes

POST y PUT reciben:

```json
{ "placa": "ABC-123", "marca": "Toyota", "modelo": "Yaris", "anio": 2024, "clienteId": 1 }
```

La respuesta agrega `id` y `clienteNombre`, sin anidar entidades.
Placa: tres letras mayusculas y tres digitos, año: 1990–2027, marca y modelo: obligatorios, maximo 40 caracteres.
La placa repetida produce 409; cliente inexistente, 404.
DELETE rechaza con 409 cualquier vehiculo con ordenes, incluidas las anuladas.

GET `/clientes` devuelve `id, documento, nombres, telefono`. Los clientes son de solo lectura.

### Ordenes

```json
{
  "vehiculoId": 1,
  "kilometraje": 45001,
  "detalles": [
    { "servicioId": 1, "cantidad": 2 },
    { "servicioId": 2, "cantidad": 1 }
  ]
}
```

La respuesta incluye `id, numero, fecha, vehiculoId, placa, clienteNombre, kilometraje, estado, total,
usuarioRegistro, motivoAnulacion, detalles`.
Cada detalle incluye `id, servicioId, codigo, servicio, cantidad, precioUnitario, subtotal`.

- La orden admite 1–10 detalles, sin servicios repetidos, con cantidades 1–5.
- El kilometraje admite 0–9 999 999 y debe ser al menos el de la ultima orden REGISTRADA del vehiculo.
- Un servicio inactivo produce 409; un servicio o vehiculo inexistente, 404.
- Los precios salen del catalogo en Oracle. La API calcula subtotales y total, y obtiene el usuario del token.
- `@Transactional` engloba cabecera y detalles. Las excepciones revierten ambos.
- Los bloqueos del vehiculo y los servicios evitan registros con kilometrajes incoherentes y cambios de precio durante la operacion.
- La secuencia Oracle genera numeros `OS-000001`. La semilla usa 1–4 y las altas empiezan desde 5.
  Puede haber saltos despues de pruebas o rollback; la secuencia no se revierte y no se reutilizan numeros.

PATCH `/ordenes/{id}/anular` recibe `{ "motivo": "Cliente cancelo la atencion" }`.
Solo admite ordenes REGISTRADAS del mismo dia, segun America/Lima; motivo: 10–200 caracteres.
La anulación conserva la orden y sus detalles para el historial.

Filtros opcionales y combinables: placa (coincidencia parcial sin distinguir mayusculas), estado
(`REGISTRADA` o `ANULADA`), desde y hasta (`yyyy-MM-dd`, ambos dias incluidos).
Las ordenes se devuelven por fecha descendente; desde posterior a hasta produce 400.

### Reporte

El reporte devuelve `codigo, servicio, cantidadTotal, importeTotal`.
Se calcula con JPQL `SUM` y `GROUP BY` sobre detalles de ordenes REGISTRADAS, ordenado por importe descendente.
Los filtros de fechas son opcionales y ambos dias son inclusivos. Las ordenes ANULADAS quedan excluidas.

### Formato de error

```json
{
  "timestamp": "2026-10-29T09:14:22",
  "status": 400,
  "error": "Bad Request",
  "mensaje": "Datos invalidos",
  "ruta": "/api/v1/vehiculos",
  "campos": { "placa": "La placa debe tener el formato ABC-123" }
}
```

`campos` aparece solo en errores de Bean Validation; en detalles puede usar claves como `detalles[0].cantidad`.

## Conexion con Angular

En el frontend desarrollado en Visual Studio Code:

```typescript
export const environment = { apiUrl: 'http://localhost:8080/api/v1' };
```

El origen autorizado es `http://localhost:4200`. Se permiten GET, POST, PUT, PATCH, DELETE, OPTIONS y Authorization.
Envía el token desde el interceptor. Un 403 conserva la sesion; un 401 fuera del login debe cerrarla.
El backend ya entrega roles, permisos y vencimiento para los guards y el menu.

## Semilla y demostracion

La migracion incluye 6 clientes, 8 servicios, 8 vehiculos, 3 ordenes REGISTRADAS y 1 ANULADA.
`SRV-008` esta inactivo; `AAA-111` tiene kilometraje previo 45000.
Las fechas de la semilla se calculan respecto al primer arranque. Las ordenes previas son de dias anteriores.

Importa `postman/AutoAndes.postman_collection.json` y, opcionalmente, el environment local.
Ejecuta las seis carpetas en orden; guardan automaticamente tokens e identificadores.
Hay 52 solicitudes y ejemplos reales guardados para 200, 201, 400, 401, 403, 404 y 409.
La carpeta de ordenes deja una orden ANULADA como historial. Los servicios y vehiculos de demostracion se eliminan.
El total esperado de 320 supone que se mantienen los precios semilla (120 y 80).

En SQL Developer, conecta como AUTODB y usa `sql/verificacion.sql` para observar cabeceras, detalles y totales,
y comparar los conteos antes/despues de rechazar el tercer detalle inactivo.

## Pruebas

Pruebas unitarias, sin necesitar Oracle:

```powershell
.\mvnw.cmd test
```

Suite completa, con pruebas HTTP contra Oracle real en un puerto aleatorio:

```powershell
$env:RUN_ORACLE_TESTS = 'true'
.\mvnw.cmd verify
Remove-Item Env:RUN_ORACLE_TESTS
```

La suite completa contiene 22 pruebas: 14 unitarias y 8 de integracion.
Verifica CRUD, errores comunes, roles, BCrypt, JWT alterado/vencido, filtros, CORS, Swagger,
calculos en el servidor, anulación, reporte y ausencia de escrituras ante un detalle inactivo.
Tambien fuerza un fallo despues de los INSERT y comprueba en Oracle que cabecera y detalles se revirtieron.
Limpia exclusivamente los registros creados por las pruebas; las secuencias pueden avanzar.
Ejecuta las pruebas sobre la semilla sin modificar precios ni registrar ordenes adicionales para AAA-111.
Los resultados Maven estan en `target/surefire-reports`.

## Recorrido para la defensa

1. `OrdenController` valida el Request y obtiene el usuario del token.
2. `OrdenServiceImpl.registrar` aplica las reglas, toma bloqueos y calcula importes dentro de la transaccion.
3. `OrdenServicioRepository.saveAndFlush` guarda la cabecera y, por cascade, sus detalles.
4. `OrdenMapper` convierte las entidades a DTO antes de cerrar la transaccion.
5. `GlobalExceptionHandler` convierte las excepciones en errores HTTP comunes.
6. `DetalleOrdenRepository.ingresos` contiene la consulta JPQL agregada del reporte.
7. `JwtAuthenticationFilter` valida el token; `SecurityConfig` comprueba permisos antes del controlador.

La distribucion es `config, security, entity, repository, service, service/impl, controller, dto, mapper, exception`.

## Git y entrega

Repositorio local con ramas `main`, `develop` y `examen-a-alarcon`.
El desarrollo se hizo en la rama de examen con commits por las tres preguntas y por las pruebas/documentacion.
Al terminar se integran las ramas y se crea la etiqueta anotada `v1.0-examen-u2`.

No hay remoto configurado. Cuando crees el repositorio vacio autoandes-backend en tu cuenta:

```powershell
git remote add origin <URL-DE-TU-REPOSITORIO>
git push -u origin main
git push origin develop examen-a-alarcon
git push origin v1.0-examen-u2
```

Este entregable cubre el backend. El repositorio frontend y el PDF de capturas de la aplicacion completa
corresponden a la siguiente etapa del trabajo.

Para produccion usa `SPRING_PROFILES_ACTIVE=prod`, DB_URL, DB_USERNAME, DB_PASSWORD y JWT_SECRET
(Base64 de una clave aleatoria de al menos 32 bytes); configura CORS_ORIGINS para el dominio del frontend.

