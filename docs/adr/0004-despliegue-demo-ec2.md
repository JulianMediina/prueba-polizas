# ADR 0004 – Despliegue demo en una EC2 con CloudFront

- **Estado:** aceptada
- **Fecha:** 2026-10-15

## Contexto

La aplicación debe quedar funcional y pública durante **4 días** para la evaluación, con el menor costo posible y
con toda la infraestructura definida como código (Terraform). La arquitectura objetivo (EKS, Aurora, EventBridge)
cuesta del orden de cientos de dólares al mes y tarda en aprovisionarse.

## Decisión

| Necesidad | Solución demo | Equivalente en producción |
|---|---|---|
| Cómputo | 1 EC2 **t4g.small** (Graviton) con Docker Compose | EKS Multi-AZ |
| Base de datos | PostgreSQL 16 en contenedor, volumen EBS cifrado | Aurora PostgreSQL Multi-AZ |
| HTTPS sin dominio | **CloudFront** con el certificado `*.cloudfront.net` | Route 53 + ACM + CloudFront |
| Protección del origen | SG solo con la prefix list de CloudFront + header secreto `X-Origin-Verify` | ALB interno + VPC Link |
| Acceso a la instancia | **SSM** (sin SSH, sin puerto 22) | SSM / bastion-less |
| Secretos | SSM Parameter Store (SecureString), generados por Terraform | Secrets Manager con rotación |
| Imágenes | **ECR** con scan on push; la instancia descarga con su rol IAM | ECR |
| CI/CD | GitHub Actions + **OIDC** (sin llaves estáticas) | Igual |

Se eligió **ECR en lugar de GHCR**: el repositorio es privado, y con GHCR la instancia necesitaría un token de
GitHub para descargar imágenes. Con ECR la autenticación es el rol IAM de la instancia, sin secretos que
distribuir; el costo es de centavos.

Costo estimado para 4 días: EC2 ≈ US$1,6 + IPv4 pública ≈ US$0,5 + EBS ≈ US$0,2 + CloudFront/ECR/S3 ≈ US$0
(capa gratuita) → **menos de US$3**.

## Consecuencias

- ✅ Barato, reproducible (`terraform apply`) y destruible con un workflow (`destroy.yml`).
- ⚠️ Sin alta disponibilidad: una sola instancia en una zona. Aceptable para una demo; no para producción.
- ⚠️ Postgres en contenedor: los datos sobreviven a reinicios, pero no a reemplazar la instancia.
