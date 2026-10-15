# Repositorios ECR. La instancia los consume con su rol IAM: no hay tokens de registro que distribuir.

resource "aws_ecr_repository" "this" {
  for_each             = toset(var.repositorios)
  name                 = "${var.prefijo}-${each.key}"
  image_tag_mutability = "IMMUTABLE"
  force_delete         = true # entorno efímero: el destroy elimina también las imágenes

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }
}

resource "aws_ecr_lifecycle_policy" "this" {
  for_each   = aws_ecr_repository.this
  repository = each.value.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Conservar las últimas ${var.imagenes_a_conservar} imágenes"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = var.imagenes_a_conservar
      }
      action = { type = "expire" }
    }]
  })
}
