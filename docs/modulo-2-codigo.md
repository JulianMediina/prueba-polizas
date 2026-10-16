# Módulo 2 – Prueba técnica práctica

**Repositorio:** <https://github.com/JulianMediina/prueba-polizas>

**Demo en línea:** <https://d3l78drj5ly5ij.cloudfront.net> (disponible durante el periodo de evaluación)

El código, las instrucciones de ejecución y los ejemplos `curl` de todos los endpoints están en el
[README del backend](../backend/README.md). Resumen de lo entregado:

| Pedido del enunciado | Dónde está |
|---|---|
| Estructura Spring Boot por capas (controller, service, repository) | [`backend/src/main/java/com/aseguradora/polizas`](../backend/src/main/java/com/aseguradora/polizas) |
| Entidades básicas `Poliza` y `Riesgo` | [`domain/Poliza.java`](../backend/src/main/java/com/aseguradora/polizas/domain/Poliza.java), [`domain/Riesgo.java`](../backend/src/main/java/com/aseguradora/polizas/domain/Riesgo.java) |
| 6 endpoints | [`PolizaController`](../backend/src/main/java/com/aseguradora/polizas/controller/PolizaController.java), [`RiesgoController`](../backend/src/main/java/com/aseguradora/polizas/controller/RiesgoController.java) |
| Validaciones de negocio | En el dominio (`Poliza`), cubiertas por tests unitarios y de integración |
| Mock `POST /core-mock/evento` | [`CoreMockController`](../backend/src/main/java/com/aseguradora/polizas/controller/CoreMockController.java), invocado después de cada cambio de estado |
| Header `x-api-key` | [`ApiKeyAuthFilter`](../backend/src/main/java/com/aseguradora/polizas/config/ApiKeyAuthFilter.java) (401 sin key o con key inválida) |
| README con instrucciones | [`backend/README.md`](../backend/README.md) y [`README.md`](../README.md) |

**Adicional:** interfaz web en Angular, 64 tests en el backend con cobertura ≥ 80 %, CI en GitHub Actions
(tests, e2e con Docker Compose y escaneo de seguridad) y despliegue en AWS definido con Terraform.

**Ejecución local:** `cp .env.example .env` (con `API_KEY=123456`) y `docker compose up --build`.
Frontend en `http://localhost:8080`; API directa en `http://localhost:8081` con el header `x-api-key: 123456`.
