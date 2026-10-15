variable "nombre" {
  description = "Prefijo de nombres de los recursos"
  type        = string
}

variable "cidr" {
  description = "Bloque CIDR de la VPC"
  type        = string
  default     = "10.20.0.0/24"
}

variable "zona" {
  description = "Zona de disponibilidad de la subred pública"
  type        = string
  default     = "us-east-1a"
}
