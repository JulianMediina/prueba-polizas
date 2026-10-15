# ADR 0003 – Arquitectura hexagonal y capa anticorrupción para el CORE

- **Estado:** aceptada
- **Fecha:** 2026-10-15

## Contexto

El CORE de seguros es un sistema legado expuesto por un servicio agnóstico de edición en WebLogic (capa media).
Su contrato, sus tiempos de respuesta y sus códigos de error no deben filtrarse al dominio de pólizas, y la
plataforma debe poder probarse sin él.

## Decisión

- El dominio define el puerto `CorePolizasPort` (*qué* se informa: evento, póliza, riesgo, operación).
- Los adapters implementan el *cómo*: hoy `CoreHttpAdapter` (REST contra `/core-mock/evento`); mañana un adapter
  SOAP/JMS para WebLogic sin tocar el dominio.
- El adapter concentra resiliencia (timeout, retry con backoff exponencial y *jitter*, circuit breaker), traducción
  de errores y propagación del `correlationId`.

## Consecuencias

- ✅ Dominio y tests independientes del CORE (en los tests de integración el puerto se reemplaza por un mock).
- ✅ Cambiar el protocolo o el proveedor es un cambio localizado.
- ⚠️ Una capa de mapeo adicional que mantener.
