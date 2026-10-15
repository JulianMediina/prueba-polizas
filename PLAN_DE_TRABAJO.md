# Plan de trabajo – Prueba Técnica Desarrollador TI / Sénior

> **Caso:** Plataforma de Gestión de Pólizas de Arrendamiento (Individual / Colectiva)
> **Dedicación estimada:** 1–2 días (≈ 14–16 h efectivas)
> **Entrega:** Monorepo en GitHub

---

## 0. Decisiones tomadas

| Tema | Decisión | Motivo |
|---|---|---|
| Backend | **Java 21 + Spring Boot 3.x + Maven Wrapper** | Lo exige el Módulo 2. Java 21 es LTS (records, pattern matching, virtual threads). |
| Arquitectura backend | **Capas (controller / service / repository) + puerto/adapter para el CORE** | Cumple el entregable al pie de la letra y deja ver criterio hexagonal sin sobreingeniería. Dominio rico: las reglas viven en las entidades. |
| Base de datos | **PostgreSQL 16 en Docker + Flyway** | Realista y reproducible. Tests de integración con Testcontainers. |
| IPC | **Tabla `ipc(anio, porcentaje)` cargada por Flyway** | La regla de negocio queda en el servidor, es auditable y se puede probar. |
| Frontend | **Angular (versión estable actual, ≥ 18), standalone + Signals + Angular Material (tema M3 propio)** | Encaja con un stack corporativo Java; tipado estricto. |
| Línea visual | **Corporativo seguros**: azul petróleo / verde azulado / ámbar; **Inter** + **JetBrains Mono** | Transmite solidez y confianza; contraste WCAG AA. |
| Nube (Módulo 1) | **AWS** | EKS, API Gateway, EventBridge/SQS, Aurora PostgreSQL, SES/SNS, Secrets Manager. |
| Módulo 3 | **Oracle (PL/SQL) + notas de equivalencia en PostgreSQL** | El enunciado menciona PL/SQL y WebLogic, lo que apunta a un ecosistema Oracle. |
| Calidad | **Tests unitarios + integración, CI en GitHub Actions, escaneo de seguridad** (Gitleaks, Dependabot, Trivy) | Evidencia de prácticas sénior. Se descarta OWASP Dependency-Check (lento y requiere API key de NVD). |
| Documentación API | Ejemplos `curl` en README. *(Opcional, si sobra tiempo: springdoc/Swagger UI.)* | — |
| Renovación | **Se actualiza la misma póliza** (canon × (1+IPC), prima, nueva vigencia, estado RENOVADA) y se guarda el historial en la tabla `renovacion` | Fiel al enunciado ("Estado pasa a RENOVADA"). Se elimina `poliza_origen_id`/`poliza_nueva_id`. |
| Alta de pólizas | **`POST /polizas`** (individual con su riesgo único, o colectiva) | Cubre "crear" del M1 y permite al evaluador probar la app desplegada. |
| Despliegue demo (4 días) | **AWS us-east-1, 1 EC2 t4g.small con docker compose** (Postgres + backend + nginx front) y **Caddy** con HTTPS automático sobre `sslip.io` (sin dominio) | Unos US$2–4 en total. La arquitectura objetivo del M1 sigue siendo la de EKS/Aurora; el despliegue demo es una simplificación documentada en un ADR. |
| IaC | **Terraform**: `infra/bootstrap` (bucket de estado, proveedor OIDC de GitHub, rol de despliegue), `infra/modules/{network,app-host}`, `infra/envs/demo`. Backend **S3 con `use_lockfile`** (sin DynamoDB) | Reproducible; sin access keys de larga duración. |
| CD | **GitHub Actions + OIDC**: build → push de imágenes a **GHCR** → `terraform apply` → despliegue en la EC2 vía **SSM** (sin SSH, sin puerto 22). Workflow manual `destroy` | Sin secretos estáticos de AWS; apagado limpio al terminar. |
| Herramientas locales | Instalación con **winget**: Temurin 21, Node LTS, Terraform, AWS CLI, gh, Docker Desktop (WSL2) | El equipo solo tenía git. |

---

## 1. Estructura del monorepo

```
prueba-polizas/
├── PLAN_DE_TRABAJO.md
├── README.md                     # Visión general, cómo ejecutar todo, índice de entregables
├── docker-compose.yml            # postgres + backend + frontend (nginx)
├── .env.example                  # Variables requeridas con valores de ejemplo (sin secretos reales)
├── .gitignore                    # Incluye .env, target/, node_modules/, dist/
├── .gitleaks.toml
├── .github/
│   ├── workflows/ci.yml          # build + test + coverage + escaneo
│   ├── dependabot.yml
│   └── pull_request_template.md
├── docs/
│   ├── modulo-1-arquitectura.md  # Diseño de sistema + diagramas Mermaid
│   ├── modulo-3-sql.md
│   ├── modulo-4-git.md
│   ├── modulo-5-liderazgo.md
│   └── adr/                      # Architecture Decision Records (0001-..., 0002-...)
├── sql/
│   ├── oracle/                   # Scripts del Módulo 3 (índices, MV, particionamiento)
│   └── postgres/                 # Equivalencias + script de benchmark con datos sintéticos
├── scripts/
│   └── demo-cherry-pick.sh       # Demostración reproducible del Módulo 4
├── backend/                      # Spring Boot (Módulo 2)
├── frontend/                     # Angular
├── deploy/
│   ├── docker-compose.prod.yml   # Imágenes de GHCR + Caddy
│   └── Caddyfile                 # HTTPS automático sobre <ip>.sslip.io
└── infra/                        # Terraform
    ├── bootstrap/                # Bucket de estado S3, proveedor OIDC de GitHub, rol de despliegue (apply local, una vez)
    ├── modules/
    │   ├── network/              # VPC, subred pública, IGW, SG (80/443, sin 22)
    │   └── app-host/             # EC2 t4g.small, IAM role SSM, EIP, user_data (Docker), SSM Parameter Store
    └── envs/demo/                # Composición + backend S3 (use_lockfile)
```

---

## 2. Cronograma

| Bloque | Fase | Tiempo | Resultado |
|---|---|---|---|
| **Día 1 – mañana** | F0. Preparación del repo y del entorno | 0,5 h | Monorepo, `.gitignore`, `.env.example`, Gitleaks, compose con Postgres |
| | F1. Módulo 1 – Diseño de sistema | 2,5 h | `docs/modulo-1-arquitectura.md` + ADRs |
| **Día 1 – tarde** | F2. Módulo 2 – Backend | 4,5 h | API funcional, probada y en contenedor |
| **Día 2 – mañana** | F3. Frontend Angular | 3,5 h | UI de gestión de pólizas conectada |
| | F4. Módulo 3 – SQL | 1 h | `docs/modulo-3-sql.md` + scripts |
| **Día 2 – tarde** | F5. Módulo 4 – Git | 0,5 h | `docs/modulo-4-git.md` + script demo |
| | F6. Módulo 5 – Liderazgo | 1 h | `docs/modulo-5-liderazgo.md` |
| | F7. CI, QA final y entrega | 1,5 h | Pipeline en verde, README final, repo publicado |
| **Transversal** | F8. IaC + CD en AWS | 2,5 h | `infra/` con Terraform, workflow de despliegue con OIDC → GHCR → SSM, app pública en HTTPS por 4 días |

> **Orden sugerido:** el Módulo 1 va primero porque fija el modelo de dominio y los contratos que se implementan en el Módulo 2. Los módulos 3, 4 y 5 son documentales y no dependen de nada; sirven para llenar huecos (por ejemplo, mientras corre el CI).

---

## 3. Lineamientos transversales de seguridad (aplican a todo)

- [ ] **Cero secretos en el repo.** Todo valor sensible (API key, credenciales de BD) se lee de variables de entorno. `.env` está en `.gitignore`; solo se versiona `.env.example`.
- [ ] La API key `123456` del enunciado **no se escribe en el código**. Se documenta en el README como el valor que va en `.env` para la evaluación.
- [ ] Comparación de la API key en **tiempo constante** (`MessageDigest.isEqual`) dentro de un filtro de Spring Security sin estado.
- [ ] **El navegador nunca ve la API key:** el nginx del frontend actúa como proxy inverso (patrón BFF ligero) y agrega el header `x-api-key` del lado del servidor, a partir de una variable de entorno. En desarrollo, `proxy.conf.js` de Angular lee la key de `process.env`.
- [ ] Usuario de BD con **mínimo privilegio** para la aplicación (DML) y uno distinto para las migraciones de Flyway (DDL).
- [ ] Errores en formato **RFC 9457 (ProblemDetail)**, sin stack traces ni detalles internos hacia el cliente.
- [ ] Validación de entrada con Jakarta Validation en todos los DTO.
- [ ] CORS restringido al origen del frontend; headers de seguridad en nginx (CSP, `X-Content-Type-Options`, `Referrer-Policy`, `X-Frame-Options`).
- [ ] Contenedores **sin usuario root**, imágenes base mínimas (`eclipse-temurin:21-jre-alpine` o distroless, `nginx-unprivileged`), versiones fijas.
- [ ] Logs sin datos personales: se enmascaran documentos, correos y teléfonos (Ley 1581 de 2012, protección de datos en Colombia).
- [ ] Fuentes tipográficas **autoalojadas** con `@fontsource`, sin CDNs externos, para que la CSP pueda ser estricta.
- [ ] Gitleaks en pre-commit y en CI; Dependabot; OWASP Dependency-Check (backend); `npm audit` (frontend); Trivy (imágenes).

---

## 4. Fase 0 – Preparación (0,5 h)

**Tareas**
1. Crear la estructura de carpetas, `git init` y la rama `main` protegida (en GitHub: PR obligatorio y CI en verde).
2. `.gitignore`, `.env.example`, `.gitleaks.toml`, `pull_request_template.md`.
3. `docker-compose.yml` con PostgreSQL 16 (credenciales desde `.env`, volumen nombrado, healthcheck).
4. Convención de commits: **Conventional Commits** (`feat:`, `fix:`, `docs:`, `test:`, `chore:`).
5. Flujo: ramas cortas `feature/*` → PR → `main` (trunk-based).

**Entregable:** repo inicial con un primer commit `chore: estructura base del monorepo`.

---

## 5. Fase 1 – Módulo 1: Diseño de sistema (2,5 h)

**Entregable:** `docs/modulo-1-arquitectura.md` (+ ADRs en `docs/adr/`), con diagramas **Mermaid**, que GitHub dibuja sin herramientas externas.

### 5.1 Contenido del documento

**1. Contexto y supuestos**
- Actores: inmobiliaria o administración de copropiedad (tomador de la póliza colectiva), arrendatario (asegurado; también tomador en la individual), arrendador (beneficiario), usuario interno de la aseguradora.
- Supuestos explícitos: el canon está en la póliza y, en las colectivas, también en cada riesgo; la prima es canon × meses de vigencia; la renovación se hace por el mismo periodo con el IPC del año anterior.

**2. Arquitectura de alto nivel (AWS)**
- **Borde:** Route 53 → CloudFront + S3 (SPA Angular) → AWS WAF → **Amazon API Gateway** (autenticación con Cognito/JWT para usuarios y API keys/planes de uso para integradores, *throttling*) → VPC Link → ALB interno.
- **Cómputo:** **Amazon EKS** (o ECS Fargate como alternativa) con los microservicios:
  - `poliza-service`: ciclo de vida de la póliza (crear, consultar, modificar, renovar, cancelar).
  - `riesgo-service`: riesgos de las pólizas colectivas (agregar, cancelar) y el riesgo único de las individuales.
  - `renovacion-job`: disparado por **EventBridge Scheduler**; selecciona las pólizas por vencer y aplica las reglas.
  - `notificacion-service`: consume eventos y envía correo (**Amazon SES**) y SMS (**Amazon SNS / End User Messaging**) a partir de plantillas.
  - `core-adapter`: **capa anticorrupción** hacia el servicio agnóstico de edición en WebLogic (capa media), que actualiza el CORE.
- **Mensajería:** **Amazon EventBridge** (bus de dominio) → colas **SQS** por consumidor, con **DLQ**. Publicación confiable con **Transactional Outbox**.
- **Datos:** **Aurora PostgreSQL** Multi-AZ (una base lógica por servicio), **ElastiCache Redis** para consultas calientes y catálogos (IPC).
- **Seguridad:** Secrets Manager (credenciales con rotación), KMS (cifrado en reposo), TLS de extremo a extremo, IAM Roles for Service Accounts (IRSA), subredes privadas y conectividad privada con el datacenter de WebLogic (Direct Connect / Site-to-Site VPN).
- **Observabilidad:** OpenTelemetry → CloudWatch Logs / Metrics + AWS X-Ray (o Amazon Managed Prometheus + Grafana).

**3. Tres patrones de arquitectura y su justificación**

| # | Patrón | Por qué en este caso |
|---|---|---|
| 1 | **Microservicios + API Gateway** | Ciclos de vida y cargas distintas (consulta intensiva frente a renovación por lotes y notificaciones); despliegue y escalado independientes; un punto único para autenticación, *rate limiting* y versionamiento. |
| 2 | **Event-Driven con Transactional Outbox** | Las notificaciones y la sincronización con el CORE no deben bloquear ni tumbar la transacción de negocio. El outbox garantiza "al menos una vez" sin transacciones distribuidas; los consumidores son idempotentes. |
| 3 | **Hexagonal / Anti-Corruption Layer para el CORE** | El CORE es legado y sus contratos (SOAP/XML en WebLogic) no deben contaminar el dominio. El puerto `CorePolizasPort` permite cambiar el adapter (mock, REST, SOAP) y probar sin el CORE. |

*Complementarios (se mencionan pero no cuentan entre los 3):* CQRS ligero para consultas (réplica de lectura y caché), Circuit Breaker / Retry / Bulkhead (Resilience4j), Saga orquestada para la renovación (renovar → CORE → notificar, con compensación).

**4. Modelo de datos principal** (diagrama ER en Mermaid)
- `poliza`: id, numero, tipo (INDIVIDUAL | COLECTIVA), estado (ACTIVA | RENOVADA | CANCELADA), tomador_id, fecha_inicio, fecha_fin, meses_vigencia, canon_mensual, prima, version (bloqueo optimista), auditoría (created_at/by, updated_at/by).
- `riesgo`: id, poliza_id, inmueble (dirección, ciudad, matrícula), arrendatario_id, arrendador_id (beneficiario), canon_mensual, estado (ACTIVO | CANCELADO), fecha_inclusion, fecha_cancelacion.
- `tercero`: id, tipo_documento, numero_documento, nombre, email, telefono (datos personales cifrados o enmascarados).
- `ipc`: anio (PK), porcentaje, fuente (DANE).
- `renovacion`: id, poliza_id, ipc_aplicado, canon_anterior, canon_nuevo, prima_anterior, prima_nueva, vigencia_anterior, vigencia_nueva, fecha.
- `outbox_event`: id, aggregate_type, aggregate_id, event_type, payload (JSONB), created_at, published_at.
- `core_sync`: id, poliza_id, riesgo_id, operacion, estado (PENDIENTE | ENVIADO | ERROR), intentos, ultimo_error, correlation_id.
- Invariantes: una póliza INDIVIDUAL tiene exactamente un riesgo; una póliza CANCELADA no se renueva ni recibe riesgos.

**5. Atributos de calidad**
- **Escalabilidad:** servicios sin estado con HPA (CPU/RPS) y KEDA según la profundidad de las colas SQS; Aurora con réplicas de lectura; Redis; renovación por lotes particionada (por fecha de vencimiento) con chunks idempotentes; paginación obligatoria en los listados.
- **Logs y observabilidad:** logs JSON estructurados con `traceId` / `correlationId` propagados (W3C Trace Context) hasta el CORE; métricas RED/USE y de negocio (pólizas renovadas, fallos con el CORE, latencia de notificaciones); trazas distribuidas; dashboards y alarmas con SLO (p. ej. 99,9 % de disponibilidad, p95 < 300 ms en consultas).
- **Tolerancia a fallos:** Multi-AZ; Resilience4j (timeout, retry con backoff exponencial y jitter, circuit breaker, bulkhead) en el `core-adapter`; outbox + DLQ + reprocesamiento; idempotencia (`Idempotency-Key` en los POST y deduplicación en los consumidores); degradación controlada (si el CORE no está disponible, la operación queda `PENDIENTE_CORE` y se reintenta); health checks (liveness/readiness), PodDisruptionBudgets, despliegues blue/green o canary.
- **Versionamiento de APIs:** versión mayor en la URI (`/api/v1/...`); cambios compatibles sin subir versión; los cambios incompatibles van a `/v2` y conviven con `/v1`, que se marca con los headers `Deprecation` y `Sunset`; contratos OpenAPI versionados en el repo con *contract testing*; versionado de esquemas de eventos (campo `schemaVersion`, solo cambios aditivos).

**6. Diagramas** (Mermaid)
- [ ] Diagrama C4 nivel 1 (Contexto).
- [ ] Diagrama C4 nivel 2 (Contenedores): API Gateway, Servicio de Pólizas, Servicio de Riesgos, Servicio de Notificaciones, Adapter CORE, base de datos, bus de eventos, WebLogic/CORE.
- [ ] Secuencia: **Agregar riesgo a una póliza colectiva** (validación → persistencia + outbox → CORE vía adapter → notificación).
- [ ] Secuencia: **Renovación automática** (Scheduler → job → IPC → nueva vigencia → CORE → notificación; ruta de fallo con DLQ).
- [ ] Diagrama ER.

**7. ADRs** (formato corto: contexto, decisión, consecuencias)
- `0001-microservicios-con-api-gateway.md`
- `0002-event-driven-con-outbox.md`
- `0003-acl-hexagonal-para-core.md`

**Definición de terminado (M1):** se responden las 5 actividades del enunciado, los 6 componentes pedidos aparecen en el diagrama y cada decisión tiene su justificación.

---

## 6. Fase 2 – Módulo 2: API de Pólizas en Spring Boot (4,5 h)

### 6.1 Stack
Java 21 · Spring Boot 3.x (Web, Data JPA, Validation, Security, Actuator) · PostgreSQL 16 · Flyway · Resilience4j · RestClient · Lombok (solo en entidades, opcional) · JUnit 5 · Mockito · AssertJ · Testcontainers · JaCoCo.

### 6.2 Estructura de paquetes

```
backend/src/main/java/com/aseguradora/polizas/
├── PolizasApplication.java
├── config/            # SecurityConfig, ApiKeyAuthFilter, CorsConfig, ResilienceConfig, AppProperties
├── controller/        # PolizaController, RiesgoController, CoreMockController
│   └── dto/           # Records de request/response + mappers
├── service/           # PolizaService, RiesgoService, IpcService
├── domain/            # Poliza, Riesgo, Ipc (entidades JPA con comportamiento), enums, excepciones de dominio
├── repository/        # PolizaRepository, RiesgoRepository, IpcRepository (Spring Data JPA)
├── integration/core/  # CorePolizasPort (interfaz) + CoreHttpAdapter (RestClient + Resilience4j) + CoreEvento
└── exception/         # GlobalExceptionHandler (ProblemDetail)
src/main/resources/
├── application.yml            # Sin secretos: usa ${API_KEY}, ${DB_URL}, ${DB_USER}, ${DB_PASSWORD}
└── db/migration/              # V1__esquema.sql, V2__ipc.sql, V3__datos_demo.sql
```

### 6.3 Contrato de la API

Todos los endpoints exigen el header `x-api-key`; sin él o con un valor incorrecto responden **401** (ProblemDetail).

| # | Método y ruta | Descripción | Respuestas |
|---|---|---|---|
| 1 | `GET /polizas?tipo=&estado=&page=&size=` | Lista pólizas filtradas por tipo y estado (filtros opcionales, paginado) | 200, 400 (enum inválido) |
| 2 | `GET /polizas/{id}/riesgos` | Riesgos de la póliza | 200, 404 |
| 3 | `POST /polizas/{id}/renovar` | Canon × (1 + IPC), prima = canon × meses, nueva vigencia, estado → RENOVADA | 200, 404, 409 (póliza cancelada) |
| 4 | `POST /polizas/{id}/cancelar` | Cancela la póliza **y todos sus riesgos** | 200, 404, 409 (ya cancelada) |
| 5 | `POST /polizas/{id}/riesgos` | Agrega un riesgo **solo si es COLECTIVA** y no está cancelada | 201, 400, 404, 422 (tipo no permitido), 409 (cancelada) |
| 6 | `POST /riesgos/{id}/cancelar` | Cancela un riesgo | 200, 404, 409 (ya cancelado), 422 (riesgo único de una individual) |
| M | `POST /core-mock/evento` | Mock del CORE: `{ "evento": "ACTUALIZACION", "polizaId": 555 }` → **solo registra en el log** | 202 |
| + | `GET /polizas/{id}` *(apoyo al front)* | Detalle de una póliza | 200, 404 |
| + | `POST /polizas` *(apoyo al front / M1 "crear")* | Crea una póliza INDIVIDUAL (con su riesgo único en el mismo request) o COLECTIVA; calcula la prima | 201, 400 |

### 6.4 Reglas de negocio (en el dominio, probadas con tests unitarios)
- [ ] Una póliza **INDIVIDUAL** solo puede tener **1 riesgo** → `Poliza.agregarRiesgo()` lanza `ReglaNegocioException`.
- [ ] **No se puede renovar una póliza CANCELADA.**
- [ ] **Cancelar una póliza cancela todos sus riesgos** (en la misma transacción).
- [ ] **Agregar un riesgo valida el tipo de póliza** (solo COLECTIVA) y que la póliza no esté cancelada.
- [ ] Renovación: IPC de la tabla `ipc` para el año anterior al inicio de la nueva vigencia; si no hay IPC configurado → 422 con un mensaje claro. Cálculos con `BigDecimal` y `RoundingMode.HALF_EVEN`.
- [ ] Supuesto documentado: el riesgo único de una póliza individual no se cancela por separado; se cancela la póliza.
- [ ] Concurrencia: `@Version` (bloqueo optimista) → 409 si hay una edición simultánea.

### 6.5 Integración con el CORE (servicio agnóstico de edición)
- Toda acción que **modifica el estado** de una póliza o un riesgo publica un evento de dominio.
- Un `@TransactionalEventListener(phase = AFTER_COMMIT)` invoca `CorePolizasPort.notificar(evento)`.
- `CoreHttpAdapter` llama a `POST /core-mock/evento` con `RestClient`, con **timeout, retry y circuit breaker** (Resilience4j) y propaga el `correlationId`.
- Si falla, se registra el error en el log y la operación de negocio **no se revierte** (en el diseño del M1 esto se resuelve con outbox; aquí se documenta como simplificación).
- `CoreMockController` registra: `CORE-MOCK evento=ACTUALIZACION polizaId=555 correlationId=...`.

### 6.6 Seguridad
- `ApiKeyAuthFilter` (`OncePerRequestFilter`) dentro de una `SecurityFilterChain` sin estado; la key se lee de `app.security.api-key: ${API_KEY}`; si la variable no existe, la aplicación **no arranca** (fail-fast con `@Validated` + `@NotBlank`).
- `/actuator/health` es público; el resto de Actuator no se expone.

### 6.7 Observabilidad
- Logs estructurados JSON (structured logging nativo de Spring Boot) con `correlationId` en el MDC (filtro que lee o genera `X-Correlation-Id`).
- Actuator: health (liveness/readiness) y métricas.

### 6.8 Pruebas
- **Unitarias (dominio y servicio):** cada regla de negocio, cálculo de la renovación con IPC, cancelación en cascada, rechazo de riesgos en pólizas individuales.
- **Integración (Testcontainers + MockMvc):** cada endpoint, caso feliz y errores (401, 404, 409, 422), y verificación de que se invoca el puerto del CORE.
- **Cobertura:** JaCoCo ≥ 80 % en `domain` y `service`.

### 6.9 Contenerización
- `backend/Dockerfile` multi-stage (compilación con Maven → JRE 21 alpine), usuario no root, `HEALTHCHECK`.
- Servicio `backend` en `docker-compose.yml`, que depende de que Postgres esté sano.

### 6.10 README del backend
Requisitos, variables de entorno (tabla), `docker compose up`, ejecución local con `./mvnw spring-boot:run`, tests, ejemplos `curl` de **todos** los endpoints (incluidos los casos de error) y supuestos de negocio.

**Definición de terminado (M2):** los 6 endpoints y el mock funcionan, las 4 reglas están cubiertas por tests, `docker compose up` levanta todo y no hay secretos en el repo.

---

## 7. Fase 3 – Frontend Angular (3,5 h)

### 7.1 Stack y arquitectura
Angular (≥ 18) standalone · Signals · Reactive Forms tipados · Angular Material con **tema M3 propio** · rutas con lazy loading · `HttpInterceptor` para correlation id y manejo global de errores (ProblemDetail → snackbar) · ESLint + Prettier · tests con Jest o Vitest.

```
frontend/src/app/
├── core/          # interceptors, servicio de errores, modelos de la API
├── shared/        # componentes de UI: chips de estado, diálogo de confirmación, pipes (moneda COP)
└── features/polizas/
    ├── data/      # PolizasApiService
    ├── lista/     # Tabla con filtros por tipo/estado y paginación
    ├── detalle/   # Datos de la póliza + tabla de riesgos + acciones
    └── riesgo-form/  # Diálogo para agregar un riesgo (solo colectivas)
```

### 7.2 Pantallas
1. **Listado de pólizas:** filtros (tipo, estado), tabla paginada con número, tipo, tomador, vigencia, canon, prima y estado (chip de color).
2. **Detalle de póliza:** tarjeta de resumen, tabla de riesgos y acciones **Renovar** / **Cancelar** (con diálogo de confirmación que muestra el impacto, p. ej. "Se cancelarán N riesgos").
3. **Agregar riesgo:** diálogo con formulario validado; el botón solo aparece en pólizas COLECTIVAS no canceladas.
4. **Cancelar riesgo:** acción por fila.
5. Estados vacío, cargando (skeleton) y error en cada vista.

### 7.3 Sistema visual: "Corporativo seguros"

| Token | Color | Uso |
|---|---|---|
| `--color-primary` | `#0B3C5D` azul petróleo | Barra superior, botones primarios, enlaces |
| `--color-secondary` | `#1D7874` verde azulado | Elementos secundarios, póliza ACTIVA |
| `--color-accent` | `#F2A541` ámbar | Llamados a la acción destacados, RENOVADA |
| `--color-bg` | `#F5F7FA` | Fondo de la aplicación |
| `--color-surface` | `#FFFFFF` | Tarjetas y tablas |
| `--color-text` | `#1F2933` | Texto principal |
| `--color-success` / `--color-warning` / `--color-error` | `#2E7D32` / `#ED6C02` / `#C62828` | Estados y mensajes |

- **Tipografía:** **Inter** (400/500/600) para la interfaz y los títulos; **JetBrains Mono** para números de póliza, IDs y montos tabulares. Autoalojadas con `@fontsource`.
- **Escala:** 12 / 14 / 16 / 20 / 24 / 32 px; espaciado en múltiplos de 8 px; bordes de 8 px.
- **Accesibilidad:** contraste AA verificado, foco visible, `aria-label` en acciones con ícono, navegación por teclado.
- Formato de moneda `es-CO` (COP) y fechas `dd/MM/yyyy`.

### 7.4 Integración segura
- En desarrollo: `proxy.conf.js` reenvía `/api` → `http://localhost:8080` e inyecta `x-api-key` desde `process.env.API_KEY`.
- En contenedor: `nginx-unprivileged` sirve la SPA y hace de proxy de `/api` al backend agregando el header desde la variable de entorno (plantilla `envsubst`). Headers CSP y de seguridad.
- **La API key nunca aparece en el bundle JS**; se verifica con un `grep` sobre `dist/` en el CI.

**Definición de terminado (front):** flujo completo listar → detalle → renovar / cancelar / agregar y cancelar riesgo, contra la API real, en `docker compose up`.

---

## 8. Fase 4 – Módulo 3: Optimización SQL (1 h)

**Entregables:** `docs/modulo-3-sql.md` + `sql/oracle/*.sql` + `sql/postgres/benchmark.sql` (opcional: demostración con `EXPLAIN ANALYZE` sobre datos sintéticos de 10 M / 500 K filas).

**Estrategias a desarrollar** (el enunciado pide mínimo 3; se presentan 6, priorizadas):
1. **Diagnóstico primero:** `EXPLAIN PLAN` / `DBMS_XPLAN.DISPLAY_CURSOR` (Oracle), `EXPLAIN (ANALYZE, BUFFERS)` (PostgreSQL); estadísticas al día con `DBMS_STATS.GATHER_TABLE_STATS` e **histogramas sobre `country`** (distribución sesgada).
2. **Índices adecuados:**
   - `customers(country, customer_id, customer_name)`: índice de cobertura; el filtro se resuelve sin tocar la tabla.
   - `orders(customer_id, order_date, total_amount, order_id)`: índice de cobertura para el join (index-only scan).
   - Verificar que exista el índice sobre la FK `orders.customer_id` (es la causa más común de la lentitud).
3. **Vista materializada** con los pedidos por país (`REFRESH FAST ON COMMIT` con logs de MV, o refresco programado según la frescura que se necesite); *query rewrite* habilitado.
4. **Particionamiento:** `orders` por rango de `order_date` (*partition pruning* para consultas por periodo) y/o *reference partitioning*; `customers` por lista de `country`.
5. **Reescritura y modelo:** normalizar `country` a un código ISO (`'MX'`) para evitar problemas de tildes y collation ("México" vs "Mexico"); seleccionar solo las columnas necesarias; paginar (`FETCH FIRST n ROWS`); filtrar por rango de fechas; evitar funciones sobre columnas indexadas (o usar índices basados en funciones).
6. **Desnormalización / caché:** copiar `country_code` en `orders` (mantenido por la aplicación o un trigger) con índice compuesto; Result Cache de Oracle o caché en la aplicación para consultas repetidas.

Para cada estrategia: **qué hace, por qué mejora (I/O, tipo de join), costo o trade-off** (espacio, carga de escritura, frescura) y el script.

---

## 9. Fase 5 – Módulo 4: Git (0,5 h)

**Entregables:** `docs/modulo-4-git.md` + `scripts/demo-cherry-pick.sh` (crea un repo temporal y reproduce el escenario).

**Respuesta central:** `git cherry-pick -x <sha-del-fix>` sobre `feature/new-login`.
- **Por qué:** aplica **solo** ese commit, sin traer el resto de `main` (que sí traerían `merge` o `rebase`). `-x` deja la trazabilidad del commit de origen.
- **Pasos:** `git fetch origin` → identificar el commit (`git log origin/main --oneline --grep "security"`) → `git switch feature/new-login` → `git cherry-pick -x <sha>` → resolver conflictos si los hay (`git cherry-pick --continue`) → ejecutar los tests → `git push`.
- **Casos especiales:** si el fix entró como merge commit: `git cherry-pick -m 1 <sha-merge>`; si son varios commits: `git cherry-pick A^..B`; si no se quiere commitear todavía: `--no-commit`.
- **Qué pasa después:** al integrar la feature en `main`, Git reconoce el cambio equivalente (mismo patch-id) y no hay conflicto; se puede verificar con `git log --cherry-pick --oneline main...feature/new-login`.
- **Alternativas descartadas y por qué:** `merge main` / `rebase main` (traen todo), copiar el cambio a mano (pierde trazabilidad).
- **Buena práctica de proceso:** los fixes de seguridad en commits atómicos y una rama `hotfix/*` para facilitar el cherry-pick.

---

## 10. Fase 6 – Módulo 5: Liderazgo técnico (1 h)

**Entregable:** `docs/modulo-5-liderazgo.md`, con respuestas concretas, accionables y medibles a las 5 preguntas.

1. **5 prioridades en las primeras 2 semanas**
   1. Estabilizar: triage de los 10 incidentes, postmortems sin culpables, top 3 de causas raíz con dueño asignado.
   2. Acotar la entrega de 3 semanas: MVP con el negocio, alcance cerrado, *feature flags* y riesgos visibles.
   3. Estándares mínimos: code review obligatorio, branch protection, CI con quality gate y Definition of Done.
   4. Inventario de la deuda técnica (40 %) priorizado por riesgo × impacto, cruzado con los incidentes.
   5. Plan de nivelación de los 2 juniors: pairing con seniors, tareas acotadas y objetivos a 30-60-90 días.
2. **Organización del equipo:** dos frentes: *feature* (5 personas, incluidos los 2 juniors en pairing) y *estabilidad/plataforma* (3 personas, con rotación por sprint); guardia de incidentes rotativa; 20 % de la capacidad reservado a deuda técnica; ceremonias livianas.
3. **Métricas:** DORA (frecuencia de despliegue, lead time, tasa de fallo en cambios, MTTR), incidentes por severidad y tendencia, tiempo de ciclo de PR y de review, cobertura y deuda en Sonar, predictibilidad del sprint (comprometido vs. entregado), satisfacción del equipo.
4. **Prácticas obligatorias:** PR con ≥ 1 aprobación (2 en servicios críticos), CI en verde, tests en las reglas críticas, Conventional Commits, ramas cortas, escaneo de secretos y dependencias, ADRs, postmortems y Definition of Done compartida.
5. **Gestión de la presión del negocio:** transparencia con datos (capacidad real frente al alcance), negociar alcance y no calidad, entregas incrementales con feature flags, calidad mínima no negociable, riesgos escalados a tiempo y demos semanales para generar confianza.

---

## 11. Fase 7 – CI, QA final y entrega (1,5 h)

### 11.1 `.github/workflows/ci.yml`
- **backend:** `./mvnw verify` (tests unitarios y de integración con Testcontainers) + reporte JaCoCo.
- **frontend:** `npm ci` → lint → test → build → verificación de que no haya API key en `dist/`.
- **seguridad:** Gitleaks sobre todo el historial; Trivy sobre las imágenes Docker.
- **dependabot.yml:** Maven, npm, GitHub Actions y Docker.

### 11.2 Checklist final
- [ ] `git clone` + `cp .env.example .env` + `docker compose up` levanta todo en un equipo limpio.
- [ ] Los 6 endpoints y el mock responden según el contrato; sin `x-api-key` → 401.
- [ ] Las 4 reglas de negocio se demuestran (tests y curl).
- [ ] El log muestra los registros `CORE-MOCK` en cada cambio de estado.
- [ ] El frontend funciona de punta a punta y no expone la key.
- [ ] Gitleaks sin hallazgos; no hay `.env` en el historial.
- [ ] CI en verde.
- [ ] README raíz con: resumen, arquitectura, cómo ejecutar, índice de entregables por módulo, supuestos y posibles mejoras.
- [ ] Documentos de los módulos 1, 3, 4 y 5 revisados (ortografía, diagramas visibles en GitHub).
- [ ] Repositorio público (o con acceso para el evaluador) y enlace enviado.

---

## 11.3 Fase 8 – IaC y despliegue en AWS (2,5 h)

**Bootstrap (una sola vez, `terraform apply` local con credenciales de administrador):**
- Bucket S3 de estado (versionado, cifrado SSE-S3, acceso público bloqueado).
- Proveedor OIDC `token.actions.githubusercontent.com` + rol `gha-deploy` con *trust* restringido a `repo:<owner>/prueba-polizas:ref:refs/heads/main` y `environment:demo`, y política de mínimo privilegio (EC2/VPC/IAM acotado/SSM/S3 de estado).

**Entorno `demo` (`infra/envs/demo`):**
- `network`: VPC /24 con 1 subred pública, IGW y SG con 80/443 abiertos (sin 22 y sin NAT Gateway, para no pagarlo).
- `app-host`: EC2 **t4g.small** (Graviton, AL2023) con EBS gp3 de 20 GB cifrado, IMDSv2 obligatorio, perfil IAM con `AmazonSSMManagedInstanceCore` + lectura de `/polizas/demo/*` en Parameter Store, Elastic IP y `user_data` que instala Docker y Compose.
- Secretos (`API_KEY`, `DB_PASSWORD`) en **SSM Parameter Store SecureString**, generados con `random_password`. No se escriben en el repo ni en los outputs.
- Tags comunes con `default_tags` (`project`, `env`, `owner`, `ttl=4d`).
- Calidad: `terraform fmt -check`, `validate`, `tflint` y `trivy config` en el CI.

**Workflow `deploy.yml` (push a `main`, environment `demo`):**
1. Build y push de las imágenes `backend` y `frontend` (multi-arch `linux/arm64`) a **GHCR**, etiquetadas con el SHA.
2. `aws-actions/configure-aws-credentials` con OIDC → `terraform init/plan/apply` en `envs/demo`.
3. `aws ssm send-command`: copia `docker-compose.prod.yml` y el `Caddyfile`, lee los parámetros SSM para armar el `.env` en la instancia, ejecuta `docker compose pull && up -d` y hace un *smoke test* a `/actuator/health`.
4. Publica la URL `https://<eip-con-guiones>.sslip.io` en el resumen del job.

**Workflow `destroy.yml`:** `workflow_dispatch` con confirmación escrita → `terraform destroy` (el bootstrap se conserva).

**Costo estimado (4 días):** EC2 t4g.small ≈ US$1,6 + IPv4 pública ≈ US$0,5 + EBS ≈ US$0,3 → **< US$3**. GHCR es gratis en repos públicos.

---

## 12. Matriz de entregables

| Módulo | Entregable | Ubicación |
|---|---|---|
| 1 – Diseño de sistema | Arquitectura AWS, 3 patrones, modelo de datos, atributos de calidad, diagramas, ADRs | `docs/modulo-1-arquitectura.md`, `docs/adr/` |
| 2 – Código | API Spring Boot en capas, entidades, reglas, mock del CORE, seguridad, tests, Docker, README | `backend/`, `docker-compose.yml` |
| 2 – Interfaz | SPA Angular con sistema visual corporativo | `frontend/` |
| 3 – BBDD | 6 estrategias de optimización + scripts Oracle/PostgreSQL | `docs/modulo-3-sql.md`, `sql/` |
| 4 – Git | Estrategia con cherry-pick + demo reproducible | `docs/modulo-4-git.md`, `scripts/` |
| 5 – Liderazgo | Respuestas a las 5 preguntas | `docs/modulo-5-liderazgo.md` |
| Transversal | CI/CD, escaneo de seguridad, README raíz | `.github/`, `README.md` |
| Transversal | IaC Terraform + despliegue demo en AWS (URL pública) | `infra/`, `deploy/`, `.github/workflows/deploy.yml` |

---

## 13. Riesgos del plan y mitigación

| Riesgo | Mitigación |
|---|---|
| Docker no disponible en el equipo del evaluador | README con ejecución alternativa (Postgres local + `./mvnw spring-boot:run` + `ng serve`). |
| Tiempo insuficiente para el front | Priorizar listado + detalle + acciones; dejar el pulido visual para el final. |
| Ambigüedades del enunciado (canon por riesgo, cancelar el riesgo único) | Supuestos explícitos en el README y en el M1. |
| Conflictos de versiones (Angular / Spring Boot) | Versiones fijadas; `mvnw` y `package-lock.json` versionados. |
