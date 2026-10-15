# Host de la aplicación: una EC2 Graviton con Docker Compose (postgres + backend + frontend).
# - Sin SSH ni key pair: acceso y despliegue por SSM.
# - IMDSv2 obligatorio, disco cifrado, créditos de CPU "standard" (sin cargos por ráfagas).
# - Los secretos se generan aquí y viven en SSM Parameter Store (SecureString); nunca en el repositorio.

data "aws_region" "actual" {}
data "aws_caller_identity" "actual" {}

locals {
  ruta_parametros = "/${var.proyecto}/${var.entorno}"
  arn_parametros  = "arn:aws:ssm:${data.aws_region.actual.region}:${data.aws_caller_identity.actual.account_id}:parameter${local.ruta_parametros}"
}

# ---------------------------------------------------------------------------
# Secretos
# ---------------------------------------------------------------------------
resource "random_password" "secretos" {
  for_each = toset(["api-key", "db-owner-password", "db-app-password", "origin-secret"])
  length   = 40
  special  = false
}

resource "aws_ssm_parameter" "secretos" {
  for_each    = random_password.secretos
  name        = "${local.ruta_parametros}/${each.key}"
  description = "Generado por Terraform; no editar a mano"
  type        = "SecureString"
  value       = each.value.result
}

# ---------------------------------------------------------------------------
# IAM de la instancia
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "confianza_ec2" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "host" {
  name               = "${var.nombre}-host"
  assume_role_policy = data.aws_iam_policy_document.confianza_ec2.json
}

resource "aws_iam_role_policy_attachment" "ssm_core" {
  role       = aws_iam_role.host.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_iam_policy_document" "host" {
  statement {
    sid       = "LeerSecretosDelEntorno"
    actions   = ["ssm:GetParameter", "ssm:GetParameters"]
    resources = ["${local.arn_parametros}/*"]
  }
  statement {
    sid       = "DescifrarViaSsm"
    actions   = ["kms:Decrypt"]
    resources = ["*"]
    condition {
      test     = "StringEquals"
      variable = "kms:ViaService"
      values   = ["ssm.${data.aws_region.actual.region}.amazonaws.com"]
    }
  }
  statement {
    sid       = "TokenEcr"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }
  statement {
    sid       = "DescargarImagenes"
    actions   = ["ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer", "ecr:BatchCheckLayerAvailability"]
    resources = var.ecr_repository_arns
  }
}

resource "aws_iam_role_policy" "host" {
  name   = "host"
  role   = aws_iam_role.host.id
  policy = data.aws_iam_policy_document.host.json
}

resource "aws_iam_instance_profile" "host" {
  name = "${var.nombre}-host"
  role = aws_iam_role.host.name
}

# ---------------------------------------------------------------------------
# Instancia
# ---------------------------------------------------------------------------
data "aws_ssm_parameter" "ami_al2023_arm64" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-arm64"
}

resource "aws_instance" "host" {
  ami                         = data.aws_ssm_parameter.ami_al2023_arm64.value
  instance_type               = var.tipo_instancia
  subnet_id                   = var.subnet_id
  vpc_security_group_ids      = [var.security_group_id]
  iam_instance_profile        = aws_iam_instance_profile.host.name
  associate_public_ip_address = true

  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    compose_version = var.compose_version
  })
  user_data_replace_on_change = true

  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
    http_endpoint               = "enabled"
  }

  credit_specification {
    cpu_credits = "standard"
  }

  root_block_device {
    volume_type           = "gp3"
    volume_size           = var.disco_gb
    encrypted             = true
    delete_on_termination = true
  }

  tags = { Name = "${var.nombre}-host" }

  lifecycle {
    # Una AMI nueva no debe reemplazar la instancia (y borrar los datos) en cada apply
    ignore_changes = [ami]
  }

  depends_on = [aws_iam_role_policy.host, aws_iam_role_policy_attachment.ssm_core]
}
