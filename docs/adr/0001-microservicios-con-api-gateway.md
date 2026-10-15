# ADR 0001 – Microservicios con API Gateway

- **Estado:** aceptada
- **Fecha:** 2026-10-15

## Contexto

La plataforma combina cargas con perfiles muy distintos: consultas frecuentes del back-office y de portales de
inmobiliarias, una renovación por lotes que se concentra en ventanas nocturnas y notificaciones en ráfagas. Los
consumidores son de dos tipos (personas autenticadas e integradores máquina a máquina) y se requiere disponibilidad
24/7.

## Decisión

Separar la plataforma en servicios por capacidad de negocio (Pólizas, Riesgos, Renovación, Notificaciones,
Adapter CORE) desplegados en Amazon EKS, detrás de **Amazon API Gateway**, que concentra autenticación (Cognito
JWT y API keys con planes de uso), *throttling* por consumidor, validación básica y versionado por ruta (`/v1`).

Se empieza con **Pólizas y Riesgos en un mismo servicio** (comparten invariantes) y se separan solo si la carga lo
justifica.

## Consecuencias

- ✅ Escalado y despliegue independientes; un pico de renovaciones no degrada las consultas.
- ✅ Políticas de seguridad y cuotas en un único punto.
- ⚠️ Más componentes que operar: se mitiga con plantillas de servicio, observabilidad común y IaC.
- ⚠️ Consistencia entre servicios vía eventos (ver ADR 0002).
