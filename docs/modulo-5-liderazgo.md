# Módulo 5 – Liderazgo técnico y gestión

**Situación:** 8 desarrolladores · 40 % de deuda técnica en servicios clave · 10 incidentes críticos en el último
mes · entrega comprometida en 3 semanas · sin estándares y con code review irregular · 2 juniors con brechas
importantes.

**Lectura del problema:** los 10 incidentes y la falta de estándares son síntomas de la misma causa (cambios que
llegan a producción sin control suficiente). Si no se estabiliza primero, la entrega de 3 semanas se va a consumir
apagando incendios. La estrategia es **estabilizar y poner un mínimo de disciplina sin detener la entrega**,
negociando alcance y no calidad.

## 1. Cinco prioridades para las primeras 2 semanas

| # | Prioridad | Acciones concretas | Resultado esperado al día 14 |
|---|---|---|---|
| 1 | **Contener los incidentes** | Triage de los 10 incidentes; postmortems sin culpables (30 min cada uno); identificar las 3 causas raíz más repetidas y asignar dueño y fecha a cada una; guardia rotativa con runbook mínimo. | Top 3 causas con plan; ningún incidente crítico repetido por la misma causa. |
| 2 | **Acotar la entrega de 3 semanas** | Sesión con negocio y producto para definir el MVP: qué es imprescindible el día 21 y qué puede ir en una segunda entrega. Desglose en historias pequeñas, *feature flags* y riesgos visibles. | Alcance firmado, plan por semana y demo semanal acordada. |
| 3 | **Estándares mínimos no negociables** | *Branch protection* en `main`, PR con al menos 1 aprobación, CI en verde obligatorio (build + tests + análisis estático), Definition of Done escrita y plantilla de PR. Se introducen en la semana 1, no se discuten cada vez. | 100 % de los cambios a `main` vía PR revisado. |
| 4 | **Mapa de la deuda técnica** | Inventario del 40 % priorizado por **riesgo × impacto** y cruzado con los incidentes: primero la deuda que causa incidentes o que bloquea la entrega. Lo demás queda en un backlog visible. | Backlog de deuda priorizado y las 5 primeras tareas en curso. |
| 5 | **Plan para los juniors** | *Pairing* diario con un senior asignado (mentor), tareas acotadas y bien especificadas, reviews como espacio de aprendizaje, objetivos a 30-60-90 días. | Cada junior entrega al menos una historia con review aprobado sin retrabajo mayor. |

## 2. Organización del equipo para mejorar velocidad y calidad

- **Dos frentes con capacidad explícita:**
  - **Entrega (5 personas):** 3 seniors/semi-seniors + los 2 juniors en *pairing*. Foco en el MVP de 3 semanas.
  - **Estabilidad/plataforma (3 personas):** incidentes, causas raíz, deuda que genera incidentes, CI/CD y
    observabilidad. **Rotan** cada sprint para que todos conozcan los servicios clave y no se cree un silo.
- **Guardia rotativa** semanal (primario/secundario) con runbooks; quien está de guardia no toma historias críticas.
- **20 % de la capacidad reservada a deuda técnica** después de la entrega; durante las 3 semanas, solo la deuda
  que bloquea la entrega o causa incidentes.
- **Dueños por servicio** (*ownership*): cada servicio clave tiene un responsable técnico que aprueba cambios de
  arquitectura.
- **Ceremonias livianas:** daily de 15 min, planning semanal, demo semanal con negocio y retro quincenal.
  *Tech sync* semanal de 30 min para decisiones técnicas (registradas como ADR).

## 3. Métricas para evaluar el desempeño del área

| Dimensión | Métrica | Meta inicial (90 días) |
|---|---|---|
| Entrega (DORA) | Frecuencia de despliegue | De semanal a varias veces por semana |
| | *Lead time* de cambios (commit → producción) | < 3 días |
| Estabilidad (DORA) | Tasa de fallo en cambios | < 15 % |
| | MTTR (tiempo de recuperación) | < 4 horas en incidentes críticos |
| Calidad | Incidentes críticos por mes y tendencia | De 10 a ≤ 3 |
| | Cobertura de tests en reglas críticas · deuda en Sonar | ≥ 80 % en dominio · tendencia a la baja |
| Flujo | Tiempo de ciclo de PR · tiempo hasta el primer review | < 24 h hasta el primer review |
| Predictibilidad | Comprometido vs. entregado por sprint | ≥ 80 % |
| Personas | Satisfacción del equipo (encuesta corta mensual) · progreso de los juniors | Tendencia positiva |

Las métricas se usan para **mejorar el sistema, no para evaluar individuos**; se revisan en la retro y se
comparten con negocio de forma transparente.

## 4. Prácticas técnicas obligatorias

1. **Code review obligatorio:** PR con ≥ 1 aprobación (2 en servicios críticos), PR pequeños (< 400 líneas),
   checklist en la plantilla.
2. **CI como puerta de calidad:** build, tests, análisis estático (Sonar), escaneo de secretos y de dependencias;
   no se mergea en rojo.
3. **Tests en las reglas de negocio críticas** y tests de regresión para cada incidente corregido.
4. **Ramas de vida corta** (2–3 días como máximo) y *feature flags* para integrar temprano lo que aún no está listo
   para el usuario.
5. **Conventional Commits** y versionado semántico; changelog generado.
6. **Definition of Done compartida:** código revisado, tests, documentación mínima, observabilidad (logs, métricas,
   alertas) y despliegue verificado.
7. **Postmortems sin culpables** para todo incidente crítico, con acciones que tienen dueño y fecha.
8. **ADRs** para decisiones de arquitectura; IaC para toda la infraestructura.
9. **Seguridad desde el pipeline:** sin secretos en el código, dependencias actualizadas (Dependabot), mínimo
   privilegio.
10. **Gitflow como modelo de ramas:** encaja con un equipo que hoy no tiene estándares, entrega por versiones y
    convive con un CORE legado que exige releases planificados y validados.

    | Rama | Sale de | Se integra en | Para qué |
    |---|---|---|---|
    | `main` | — | — | Solo lo que está en producción; cada merge lleva un tag `vX.Y.Z` |
    | `develop` | `main` | `release/*` | Integración continua del sprint; siempre debe compilar y pasar CI |
    | `feature/<ticket>-<desc>` | `develop` | `develop` (PR) | Una historia por rama, vida corta, detrás de *feature flag* si no está lista |
    | `release/X.Y.0` | `develop` | `main` + `develop` | Congela el alcance: solo correcciones, QA y notas de versión |
    | `hotfix/X.Y.Z` | `main` | `main` + `develop` (+ `release/*` abierta) | Incidentes críticos en producción; se despliega sin esperar al sprint |

    ```mermaid
    gitGraph
      commit id: "v1.0.0" tag: "v1.0.0"
      branch develop
      checkout develop
      commit id: "base sprint"
      branch feature/POL-42-renovacion
      checkout feature/POL-42-renovacion
      commit id: "renovación IPC"
      checkout develop
      merge feature/POL-42-renovacion id: "PR #42"
      branch release/1.1.0
      checkout release/1.1.0
      commit id: "fix QA"
      checkout main
      merge release/1.1.0 id: "v1.1.0" tag: "v1.1.0"
      checkout develop
      merge release/1.1.0 id: "back-merge"
      checkout main
      branch hotfix/1.1.1
      checkout hotfix/1.1.1
      commit id: "fix seguridad"
      checkout main
      merge hotfix/1.1.1 id: "v1.1.1" tag: "v1.1.1"
      checkout develop
      merge hotfix/1.1.1 id: "hotfix a develop"
    ```

    **Reglas que lo hacen funcionar:**
    - `main` y `develop` protegidas: solo se entra por PR con CI en verde y al menos una aprobación (dos en
      `release/*` y `hotfix/*`).
    - *Squash merge* de `feature/*` hacia `develop` (historia limpia). Los merges de `release/*` y `hotfix/*` son
      merges normales con tag, para conservar la trazabilidad de la versión.
    - **Todo `hotfix` vuelve a `develop`** (y a la `release` abierta) el mismo día. Si una feature en curso necesita
      el fix antes, se trae con `git cherry-pick -x` (ver [Módulo 4](modulo-4-git.md)).
    - Versionado semántico con los tags de `main`; el changelog se genera a partir de los Conventional Commits.
    - Ambientes: `develop` → desarrollo, `release/*` → QA/staging, `main` → producción.
    - Para evitar la deuda típica de Gitflow (ramas largas, merges dolorosos): las features duran pocos días, una
      `release` no vive más de una semana y se revisa en cada retro si el equipo ya está listo para evolucionar a
      *trunk-based* cuando el CI y los *feature flags* estén maduros.

## 5. Gestionar la presión del negocio sin comprometer la calidad

- **Transparencia con datos:** capacidad real del equipo frente al alcance pedido, más el costo actual de los
  incidentes (horas perdidas, impacto en clientes). La conversación pasa de "¿pueden más rápido?" a "¿qué
  priorizamos?".
- **Negociar alcance, no calidad:** se define un MVP para el día 21; lo demás se entrega en incrementos. La calidad
  mínima (review, CI, tests en lo crítico) no está en la mesa, porque saltársela es lo que generó los 10 incidentes.
- **Entregas incrementales y visibles:** demo semanal con negocio y *feature flags* para desplegar temprano y activar
  cuando esté listo. Así se reduce el riesgo del "big bang" del día 21.
- **Gestión explícita de riesgos:** tablero de riesgos y escalamiento temprano (al día 7, no al día 20) si el plan se
  desvía.
- **Proteger al equipo:** un solo canal de entrada para pedidos urgentes (product owner), sin cambios de prioridad a
  mitad de semana salvo incidentes.
- **Deuda negociada como inversión:** después de la entrega, el 20 % de capacidad para deuda se presenta con su
  retorno esperado: menos incidentes, menor *lead time* y menor costo de cada funcionalidad nueva.
