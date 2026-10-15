# Bootstrap: se aplica UNA vez, localmente, con credenciales de administrador.
# Crea lo que el pipeline necesita para operar sin llaves estáticas:
#   - Bucket S3 para el estado remoto de Terraform (con bloqueo nativo por lockfile)
#   - Proveedor OIDC de GitHub Actions
#   - Rol de despliegue asumible solo desde el environment "demo" del repositorio

data "aws_caller_identity" "actual" {}
data "aws_region" "actual" {}

locals {
  cuenta    = data.aws_caller_identity.actual.account_id
  region    = data.aws_region.actual.region
  prefijo   = var.proyecto
  bucket    = "${var.proyecto}-tfstate-${local.cuenta}"
  oidc_host = "token.actions.githubusercontent.com"
}

# ---------------------------------------------------------------------------
# Estado remoto
# ---------------------------------------------------------------------------
resource "aws_s3_bucket" "tfstate" {
  bucket = local.bucket
}

resource "aws_s3_bucket_ownership_controls" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id
  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_versioning" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "tfstate" {
  bucket                  = aws_s3_bucket.tfstate.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id
  rule {
    id     = "expirar-versiones-antiguas"
    status = "Enabled"
    filter {}
    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }
}

data "aws_iam_policy_document" "tfstate_tls" {
  statement {
    sid       = "SoloTLS"
    effect    = "Deny"
    actions   = ["s3:*"]
    resources = [aws_s3_bucket.tfstate.arn, "${aws_s3_bucket.tfstate.arn}/*"]
    principals {
      type        = "*"
      identifiers = ["*"]
    }
    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
}

resource "aws_s3_bucket_policy" "tfstate" {
  bucket     = aws_s3_bucket.tfstate.id
  policy     = data.aws_iam_policy_document.tfstate_tls.json
  depends_on = [aws_s3_bucket_public_access_block.tfstate]
}

# ---------------------------------------------------------------------------
# OIDC de GitHub Actions + rol de despliegue
# ---------------------------------------------------------------------------
resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://${local.oidc_host}"
  client_id_list = ["sts.amazonaws.com"]
}

data "aws_iam_policy_document" "confianza_github" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }
    condition {
      test     = "StringEquals"
      variable = "${local.oidc_host}:aud"
      values   = ["sts.amazonaws.com"]
    }
    # Solo los jobs que corren en el environment "demo" de este repositorio
    condition {
      test     = "StringEquals"
      variable = "${local.oidc_host}:sub"
      values   = [for env in var.github_environments : "repo:${var.github_repo}:environment:${env}"]
    }
  }
}

resource "aws_iam_role" "despliegue" {
  name                 = "${local.prefijo}-gha-deploy"
  description          = "Rol asumido por GitHub Actions (OIDC) para desplegar ${var.proyecto}"
  assume_role_policy   = data.aws_iam_policy_document.confianza_github.json
  max_session_duration = 3600
}

data "aws_iam_policy_document" "despliegue" {
  statement {
    sid       = "EstadoTerraformListar"
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.tfstate.arn]
  }

  statement {
    sid       = "EstadoTerraformObjetos"
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    resources = ["${aws_s3_bucket.tfstate.arn}/envs/*"]
  }

  statement {
    sid       = "RedYComputo"
    actions   = ["ec2:*"]
    resources = ["*"]
    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [local.region]
    }
  }

  statement {
    sid = "IamSoloRecursosDelProyecto"
    actions = [
      "iam:CreateRole", "iam:DeleteRole", "iam:GetRole", "iam:TagRole", "iam:UntagRole", "iam:UpdateRole",
      "iam:UpdateAssumeRolePolicy", "iam:ListRolePolicies", "iam:ListAttachedRolePolicies",
      "iam:ListInstanceProfilesForRole", "iam:AttachRolePolicy", "iam:DetachRolePolicy", "iam:PutRolePolicy",
      "iam:GetRolePolicy", "iam:DeleteRolePolicy", "iam:CreateInstanceProfile", "iam:DeleteInstanceProfile",
      "iam:GetInstanceProfile", "iam:AddRoleToInstanceProfile", "iam:RemoveRoleFromInstanceProfile",
      "iam:TagInstanceProfile", "iam:UntagInstanceProfile",
    ]
    resources = [
      "arn:aws:iam::${local.cuenta}:role/${local.prefijo}-demo-*",
      "arn:aws:iam::${local.cuenta}:instance-profile/${local.prefijo}-demo-*",
    ]
  }

  statement {
    sid       = "PasarRolSoloAEc2"
    actions   = ["iam:PassRole"]
    resources = ["arn:aws:iam::${local.cuenta}:role/${local.prefijo}-demo-*"]
    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["ec2.amazonaws.com"]
    }
  }

  statement {
    sid = "ParametrosDelProyecto"
    actions = [
      "ssm:PutParameter", "ssm:GetParameter", "ssm:GetParameters", "ssm:DeleteParameter",
      "ssm:AddTagsToResource", "ssm:RemoveTagsFromResource", "ssm:ListTagsForResource",
    ]
    resources = ["arn:aws:ssm:${local.region}:${local.cuenta}:parameter/${var.proyecto}/*"]
  }

  statement {
    sid       = "AmiPublicaAmazonLinux"
    actions   = ["ssm:GetParameter", "ssm:GetParameters"]
    resources = ["arn:aws:ssm:${local.region}::parameter/aws/service/*"]
  }

  statement {
    sid = "SsmLecturaYComandos"
    actions = [
      "ssm:DescribeParameters", "ssm:DescribeInstanceInformation", "ssm:GetCommandInvocation",
      "ssm:ListCommandInvocations", "ssm:ListCommands",
    ]
    resources = ["*"]
  }

  statement {
    sid     = "EjecutarComandosEnLaInstanciaDelProyecto"
    actions = ["ssm:SendCommand"]
    resources = [
      "arn:aws:ec2:${local.region}:${local.cuenta}:instance/*",
    ]
    condition {
      test     = "StringEquals"
      variable = "ssm:resourceTag/proyecto"
      values   = [var.proyecto]
    }
  }

  statement {
    sid       = "DocumentoRunShellScript"
    actions   = ["ssm:SendCommand"]
    resources = ["arn:aws:ssm:${local.region}::document/AWS-RunShellScript"]
  }

  statement {
    sid       = "CifradoParametrosViaSsm"
    actions   = ["kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey"]
    resources = ["*"]
    condition {
      test     = "StringEquals"
      variable = "kms:ViaService"
      values   = ["ssm.${local.region}.amazonaws.com"]
    }
  }

  statement {
    sid       = "RegistroDeImagenes"
    actions   = ["ecr:*"]
    resources = ["arn:aws:ecr:${local.region}:${local.cuenta}:repository/${var.proyecto}-*"]
  }

  statement {
    sid       = "TokenEcr"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid       = "Cdn"
    actions   = ["cloudfront:*"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "despliegue" {
  name   = "despliegue-${var.proyecto}"
  role   = aws_iam_role.despliegue.id
  policy = data.aws_iam_policy_document.despliegue.json
}
