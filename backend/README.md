# Backend – API de gestión de pólizas (Módulo 2)

Spring Boot 4.1 · Java 21 · PostgreSQL 16 · Flyway · Resilience4j · JUnit 5 + PostgreSQL embebido.

## Estructura

```
src/main/java/com/aseguradora/polizas/
├── config/            # SecurityConfig, ApiKeyAuthFilter, CorrelationIdFilter, AppConfig (Clock, @Async + MDC)
├── controller/        # PolizaController, RiesgoController, CoreMockController
│   └── dto/           # Records de request/response + PolizaMapper
├── service/           # PolizaService, RiesgoService (casos de uso, transacciones, eventos)
├── domain/            # Poliza (agregado raíz), Riesgo, Ipc, Renovacion, enums, excepciones de dominio
├── repository/        # Spring Data JPA
├── integration/core/  # CorePolizasPort (puerto) + CoreHttpAdapter (RestClient + retry + circuit breaker)
└── exception/         # GlobalExceptionHandler (RFC 9457 ProblemDetail)
src/main/resources/db/migration/  # V1 esquema · V2 IPC (DANE) · V3 datos demo
```

Las **reglas de negocio viven en el dominio** (`Poliza`), no en los servicios: los servicios orquestan
(cargar, guardar, publicar eventos) y los controllers solo traducen HTTP ↔ DTO.

## Reglas de negocio

| Regla | Dónde | Respuesta |
|---|---|---|
| Una póliza individual solo puede tener 1 riesgo | `Poliza.crear`, `Poliza.agregarRiesgo` | 422 `riesgo-unico-individual` |
| Agregar riesgo exige validar el tipo de póliza (solo COLECTIVA) | `Poliza.agregarRiesgo` | 422 |
| No se puede renovar una póliza cancelada | `Poliza.validarRenovable` | 409 `poliza-cancelada` |
| La cancelación de una póliza cancela todos sus riesgos | `Poliza.cancelar` | 200 |
| Renovación: canon de cada riesgo × (1 + IPC del año anterior), prima = canon × meses, nueva vigencia, estado RENOVADA | `Poliza.renovar` | 200 / 422 `ipc-no-configurado` |
| Canon de la póliza = suma de riesgos activos | `Poliza.recalcularValores` | — |
| El riesgo único de una individual no se cancela por separado | `Poliza.cancelarRiesgo` | 422 |
| Una colectiva conserva al menos un riesgo activo | `Poliza.cancelarRiesgo` | 422 |
| Edición concurrente | `@Version` (bloqueo optimista) | 409 |

Montos con `BigDecimal` y redondeo `HALF_EVEN` a 2 decimales.

## Integración con el CORE

Toda acción que modifica una póliza o un riesgo publica `PolizaModificada`. Después del **commit** y en un hilo
aparte, `CoreSyncListener` llama al puerto `CorePolizasPort`; el adapter HTTP envía
`{"evento":"ACTUALIZACION","polizaId":…,"riesgoId":…,"operacion":…}` a `POST /core-mock/evento` con timeout, 3
intentos con backoff exponencial y circuit breaker. Si el CORE falla, se registra el error y la operación **no se
revierte** (en producción: outbox, ver ADR 0002 en el documento de entrega). El mock solo registra en el log:

```
CORE-MOCK evento=ACTUALIZACION polizaId=12 riesgoId=null operacion=RENOVACION
```

## Variables de entorno

| Variable | Obligatoria | Descripción |
|---|---|---|
| `API_KEY` | Sí | Valor esperado en el header `x-api-key`. Sin ella la app **no arranca**. |
| `DB_URL` | No | JDBC URL (por defecto `jdbc:postgresql://localhost:5432/polizas`) |
| `DB_APP_USER` / `DB_APP_PASSWORD` | Sí | Usuario de la aplicación (solo DML) |
| `DB_OWNER_USER` / `DB_OWNER_PASSWORD` | No | Usuario de Flyway (DDL); por defecto el mismo de la app |
| `CORE_BASE_URL` | No | URL del servicio de edición (por defecto la propia app → mock) |
| `SPRING_PROFILES_ACTIVE=prod` | No | Logs JSON estructurados (ECS) |

## Ejecutar

```bash
# Todo en contenedores (desde la raíz del repo)
cp .env.example .env        # para la evaluación: API_KEY=123456
docker compose up --build   # API directa en http://localhost:8081

# Solo el backend, con un PostgreSQL local
export API_KEY=123456 DB_APP_USER=postgres DB_APP_PASSWORD=postgres
./mvnw spring-boot:run

# Tests (unitarios + integración con PostgreSQL embebido, no requiere Docker) y cobertura
./mvnw verify               # reporte en target/site/jacoco/index.html
```

## Ejemplos con curl

```bash
API=http://localhost:8081      # o la URL pública de la demo
KEY="x-api-key: 123456"

# 1. Listar por tipo y estado (paginado)
curl -s -H "$KEY" "$API/polizas?tipo=COLECTIVA&estado=ACTIVA&page=0&size=20"

# Detalle
curl -s -H "$KEY" "$API/polizas/5"

# Crear una póliza individual (con su riesgo único)
curl -s -H "$KEY" -H "Content-Type: application/json" -X POST "$API/polizas" -d '{
  "tipo": "INDIVIDUAL", "fechaInicio": "2025-11-01", "mesesVigencia": 12,
  "tomador": {"tipoDocumento": "CC", "documento": "1020304050", "nombre": "Ana Ruiz", "email": "ana@example.com"},
  "riesgos": [{"direccion": "Calle 100 # 10-20", "ciudad": "Bogotá",
               "arrendatario": {"documento": "1020304050", "nombre": "Ana Ruiz"},
               "arrendador": {"documento": "79865432", "nombre": "Pedro Gómez"},
               "canonMensual": 2000000}]}'

# 2. Riesgos de una póliza
curl -s -H "$KEY" "$API/polizas/5/riesgos"

# 3. Renovar (canon y prima + IPC, estado RENOVADA)
curl -s -H "$KEY" -X POST "$API/polizas/1/renovar"

# 4. Cancelar (cancela también todos sus riesgos)
curl -s -H "$KEY" -X POST "$API/polizas/6/cancelar"

# 5. Agregar riesgo (solo colectivas)
curl -s -H "$KEY" -H "Content-Type: application/json" -X POST "$API/polizas/5/riesgos" -d '{
  "direccion": "Carrera 9 # 80-15 Apto 402", "ciudad": "Bogotá",
  "arrendatario": {"documento": "1010101010", "nombre": "Luis Pardo"},
  "arrendador": {"documento": "80808080", "nombre": "Marta Díaz"},
  "canonMensual": 1800000}'

# 6. Cancelar un riesgo
curl -s -H "$KEY" -X POST "$API/riesgos/5/cancelar"

# Mock del CORE
curl -s -H "$KEY" -H "Content-Type: application/json" -X POST "$API/core-mock/evento" \
  -d '{"evento": "ACTUALIZACION", "polizaId": 555}'
```

**Casos de error** (cuerpo `application/problem+json`):

```bash
curl -i "$API/polizas"                                    # 401 sin x-api-key
curl -i -H "$KEY" "$API/polizas?tipo=OTRA"                # 400 + valoresPermitidos
curl -i -H "$KEY" "$API/polizas/999999/riesgos"           # 404
curl -i -H "$KEY" -X POST "$API/polizas/3/renovar"        # 409: IND-0000003 está cancelada
curl -i -H "$KEY" -X POST "$API/polizas/2/renovar"        # 422: falta el IPC de 2026 (vigencia termina en 2027)
curl -i -H "$KEY" -H "Content-Type: application/json" \
     -X POST "$API/polizas/1/riesgos" -d '{...}'          # 422: IND-0000001 es individual
curl -i -H "$KEY" -X POST "$API/riesgos/1/cancelar"       # 422: riesgo único de una individual
```

Ejemplo de respuesta:

```json
{
  "type": "urn:polizas:error:poliza-cancelada",
  "title": "Conflicto con el estado actual",
  "status": 409,
  "detail": "No se puede renovar una póliza cancelada (IND-0000003)",
  "instance": "/polizas/3/renovar",
  "codigo": "poliza-cancelada"
}
```

## Datos de demostración (V3)

| Id | Número | Tipo | Estado | Para probar |
|---|---|---|---|---|
| 1 | IND-0000001 | Individual | Activa | Renovar (IPC 2025) · 422 al agregar riesgo |
| 2 | IND-0000002 | Individual | Activa | 422 al renovar: no hay IPC 2026 |
| 3 | IND-0000003 | Individual | Cancelada | 409 al renovar |
| 4 | IND-0000004 | Individual | Renovada | Historial de renovación |
| 5 | COL-0000005 | Colectiva | Activa | Agregar y cancelar riesgos, renovar |
| 6 | COL-0000006 | Colectiva | Activa | Cancelar en cascada |
| 7 | COL-0000007 | Colectiva | Cancelada | 409 al agregar riesgo |
| 8 | IND-0000008 | Individual | Activa | — |
