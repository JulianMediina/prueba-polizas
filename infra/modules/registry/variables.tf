variable "prefijo" {
  description = "Prefijo de los repositorios"
  type        = string
}

variable "repositorios" {
  description = "Nombres de los repositorios a crear (se les antepone el prefijo)"
  type        = list(string)
}

variable "imagenes_a_conservar" {
  description = "Cantidad de imágenes que se conservan por repositorio"
  type        = number
  default     = 10
}
