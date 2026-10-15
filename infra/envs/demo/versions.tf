terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.69"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.9"
    }
  }

  # Configuración parcial: el bucket se pasa en `terraform init -backend-config="bucket=..."`
  backend "s3" {
    key          = "envs/demo/terraform.tfstate"
    region       = "us-east-1"
    encrypt      = true
    use_lockfile = true
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      proyecto    = var.proyecto
      entorno     = var.entorno
      gestionado  = "terraform"
      propietario = var.propietario
      ttl         = "4d"
    }
  }
}
