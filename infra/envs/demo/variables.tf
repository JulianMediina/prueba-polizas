variable "region" {
  type    = string
  default = "us-east-1"
}

variable "proyecto" {
  type    = string
  default = "polizas"
}

variable "entorno" {
  type    = string
  default = "demo"
}

variable "propietario" {
  description = "Responsable del entorno (tag)"
  type        = string
  default     = "JulianMediina"
}

variable "zona" {
  type    = string
  default = "us-east-1a"
}

variable "tipo_instancia" {
  type    = string
  default = "t4g.small"
}
