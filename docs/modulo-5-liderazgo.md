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
4. **Trunk-based development** con ramas cortas y *feature flags* para lo que no está listo.
5. **Conventional Commits** y versionado semántico; changelog generado.
6. **Definition of Done compartida:** código revisado, tests, documentación mínima, observabilidad (logs, métricas,
   alertas) y despliegue verificado.
7. **Postmortems sin culpables** para todo incidente crítico, con acciones que tienen dueño y fecha.
8. **ADRs** para decisiones de arquitectura; IaC para toda la infraestructura.
9. **Seguridad desde el pipeline:** sin secretos en el código, dependencias actualizadas (Dependabot), mínimo
   privilegio.

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
