variable "region" {
  description = "Región de AWS"
  type        = string
  default     = "us-east-1"
}

variable "proyecto" {
  description = "Prefijo de nombres y valor del tag proyecto"
  type        = string
  default     = "polizas"
}

variable "github_repo" {
  description = "Repositorio autorizado a asumir el rol, formato owner/nombre"
  type        = string
  default     = "JulianMediina/prueba-polizas"
}

variable "github_environments" {
  description = "Environments de GitHub cuyos jobs pueden asumir el rol"
  type        = list(string)
  default     = ["demo"]
}

variable "github_oidc_repo" {
  description = "Repositorio en el formato inmutable del claim sub de OIDC: owner@owner_id/repo@repo_id (gh api repos/OWNER/REPO/actions/oidc/customization/sub)"
  type        = string
  default     = "JulianMediina@47359517/prueba-polizas@1421232822"
}
