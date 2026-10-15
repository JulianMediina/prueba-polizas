output "url" {
  description = "URL pública de la aplicación"
  value       = "https://${module.edge.dominio}"
}

output "instance_id" {
  value = module.app_host.instance_id
}

output "ecr_registro" {
  value = module.registry.registro
}

output "ecr_repositorios" {
  value = module.registry.urls
}

output "ruta_parametros" {
  description = "Prefijo SSM de los secretos (api-key, db-*, origin-secret)"
  value       = module.app_host.ruta_parametros
}

output "cloudfront_distribution_id" {
  value = module.edge.distribution_id
}
