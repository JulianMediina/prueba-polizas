# ADR 0002 – Event-driven con Transactional Outbox

- **Estado:** aceptada (en el código del Módulo 2 se implementa una versión simplificada)
- **Fecha:** 2026-10-15

## Contexto

Cada cambio de estado de una póliza o riesgo debe (a) informarse al CORE a través del servicio de edición de
WebLogic y (b) notificarse por correo/SMS en la creación y la renovación. Ninguna de las dos cosas debe bloquear ni
revertir la transacción de negocio, y no se pueden perder eventos si el proceso cae justo después del commit.

## Decisión

- En la misma transacción del cambio se inserta un registro en `outbox_event`.
- Un *relay* (polling o CDC con Debezium) publica en **Amazon EventBridge**; cada consumidor tiene su cola **SQS**
  con **DLQ**.
- Los consumidores son idempotentes (deduplicación por `eventId`).

## Simplificación en el código del Módulo 2

El servicio publica un evento de dominio (`PolizaModificada`) y un `@TransactionalEventListener(AFTER_COMMIT)`
asíncrono llama al CORE con retry y circuit breaker. Si el envío falla, se registra el error y la operación de
negocio **no se revierte**. Lo que falta para "al menos una vez" es justamente el outbox: si el proceso cae entre el
commit y el envío, el evento se pierde. Es una decisión consciente de alcance para la prueba.

## Consecuencias

- ✅ La API responde sin depender de la latencia ni la disponibilidad del CORE.
- ✅ Reproceso desde la DLQ y trazabilidad por `correlationId`.
- ⚠️ Consistencia eventual con el CORE: se requiere reconciliación diaria y monitoreo de la edad de la DLQ.
