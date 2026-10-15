output "tfstate_bucket" {
  description = "Bucket del estado remoto (variable TF_STATE_BUCKET en GitHub)"
  value       = aws_s3_bucket.tfstate.bucket
}

output "deploy_role_arn" {
  description = "Rol para GitHub Actions (variable AWS_ROLE_ARN en GitHub)"
  value       = aws_iam_role.despliegue.arn
}
