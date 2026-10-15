output "instance_id" {
  value = aws_instance.host.id
}

output "public_dns" {
  description = "DNS público de la instancia (origen de CloudFront)"
  value       = aws_instance.host.public_dns
}

output "ruta_parametros" {
  description = "Prefijo de los parámetros SSM con los secretos del entorno"
  value       = local.ruta_parametros
}

output "origin_secret" {
  description = "Header compartido CloudFront → origen"
  value       = random_password.secretos["origin-secret"].result
  sensitive   = true
}
