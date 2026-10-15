# Entorno "demo": despliegue efímero (4 días) de la plataforma de pólizas.
#
#   Usuario ──HTTPS──▶ CloudFront ──HTTP + X-Origin-Verify──▶ EC2 t4g.small (Docker Compose)
#                                                               ├─ frontend (nginx: SPA + BFF /api)
#                                                               ├─ backend  (Spring Boot)
#                                                               └─ postgres (volumen en EBS cifrado)
#
# La arquitectura objetivo de producción (EKS, Aurora, EventBridge...) está en docs/modulo-1-arquitectura.md;
# esta es una simplificación de costo documentada en docs/adr/0004-despliegue-demo-ec2.md.

locals {
  nombre = "${var.proyecto}-${var.entorno}"
}

module "network" {
  source = "../../modules/network"
  nombre = local.nombre
  zona   = var.zona
}

module "registry" {
  source       = "../../modules/registry"
  prefijo      = var.proyecto
  repositorios = ["backend", "frontend"]
}

module "app_host" {
  source              = "../../modules/app-host"
  nombre              = local.nombre
  proyecto            = var.proyecto
  entorno             = var.entorno
  subnet_id           = module.network.subnet_id
  security_group_id   = module.network.security_group_id
  ecr_repository_arns = module.registry.arns
  tipo_instancia      = var.tipo_instancia
}

module "edge" {
  source        = "../../modules/edge"
  nombre        = local.nombre
  origen_dns    = module.app_host.public_dns
  origin_secret = module.app_host.origin_secret
}
