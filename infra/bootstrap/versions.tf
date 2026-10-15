terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.69"
    }
  }

  # El estado del bootstrap también vive en el bucket que este mismo módulo crea.
  # Primer apply con estado local; luego: terraform init -migrate-state -backend-config=...
  backend "s3" {
    key          = "bootstrap/terraform.tfstate"
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
      gestionado  = "terraform"
      componente  = "bootstrap"
      repositorio = var.github_repo
    }
  }
}
