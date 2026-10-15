output "urls" {
  description = "URL de cada repositorio, por nombre corto"
  value       = { for k, r in aws_ecr_repository.this : k => r.repository_url }
}

output "arns" {
  value = [for r in aws_ecr_repository.this : r.arn]
}

output "registro" {
  description = "Host del registro (para docker login)"
  value       = split("/", values(aws_ecr_repository.this)[0].repository_url)[0]
}
