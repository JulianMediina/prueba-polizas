variable "nombre" {
  description = "Prefijo de nombres (p. ej. polizas-demo)"
  type        = string
}

variable "proyecto" {
  type = string
}

variable "entorno" {
  type = string
}

variable "subnet_id" {
  type = string
}

variable "security_group_id" {
  type = string
}

variable "ecr_repository_arns" {
  description = "Repositorios de los que la instancia puede descargar imágenes"
  type        = list(string)
}

variable "tipo_instancia" {
  description = "Tipo de instancia (Graviton/arm64)"
  type        = string
  default     = "t4g.small"
}

variable "disco_gb" {
  type    = number
  default = 20
}

variable "compose_version" {
  description = "Versión del plugin Docker Compose"
  type        = string
  default     = "v5.6.0"
}
