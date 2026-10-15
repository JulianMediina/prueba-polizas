variable "nombre" {
  type = string
}

variable "origen_dns" {
  description = "DNS público del origen (la instancia)"
  type        = string
}

variable "origin_secret" {
  description = "Valor del header X-Origin-Verify que CloudFront envía al origen"
  type        = string
  sensitive   = true
}
