# CloudFront delante de la instancia: HTTPS con el certificado *.cloudfront.net (sin dominio propio),
# HTTP/2 y HTTP/3, y caché de los assets estáticos. El origen solo acepta tráfico de CloudFront
# (security group con la prefix list) y además exige un header secreto (X-Origin-Verify) que valida nginx.

data "aws_cloudfront_cache_policy" "deshabilitada" {
  name = "Managed-CachingDisabled"
}

data "aws_cloudfront_cache_policy" "segun_origen" {
  name = "UseOriginCacheControlHeaders"
}

data "aws_cloudfront_origin_request_policy" "todo_menos_host" {
  name = "Managed-AllViewerExceptHostHeader"
}

data "aws_cloudfront_response_headers_policy" "seguridad" {
  name = "Managed-SecurityHeadersPolicy"
}

locals {
  origen = "app-host"
  # Rutas que se reenvían a la API sin caché: la del BFF (/api) y la API directa (exige x-api-key)
  rutas_api = ["/api/*", "/polizas", "/polizas/*", "/riesgos/*", "/core-mock/*", "/actuator/health"]
}

resource "aws_cloudfront_distribution" "this" {
  enabled         = true
  comment         = var.nombre
  is_ipv6_enabled = true
  http_version    = "http2and3"
  price_class     = "PriceClass_100"

  origin {
    origin_id   = local.origen
    domain_name = var.origen_dns

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
      origin_read_timeout    = 30
    }

    custom_header {
      name  = "X-Origin-Verify"
      value = var.origin_secret
    }
  }

  default_cache_behavior {
    target_origin_id           = local.origen
    viewer_protocol_policy     = "redirect-to-https"
    allowed_methods            = ["GET", "HEAD", "OPTIONS"]
    cached_methods             = ["GET", "HEAD"]
    compress                   = true
    cache_policy_id            = data.aws_cloudfront_cache_policy.segun_origen.id
    response_headers_policy_id = data.aws_cloudfront_response_headers_policy.seguridad.id
  }

  dynamic "ordered_cache_behavior" {
    for_each = local.rutas_api
    content {
      path_pattern               = ordered_cache_behavior.value
      target_origin_id           = local.origen
      viewer_protocol_policy     = "https-only"
      allowed_methods            = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
      cached_methods             = ["GET", "HEAD"]
      compress                   = true
      cache_policy_id            = data.aws_cloudfront_cache_policy.deshabilitada.id
      origin_request_policy_id   = data.aws_cloudfront_origin_request_policy.todo_menos_host.id
      response_headers_policy_id = data.aws_cloudfront_response_headers_policy.seguridad.id
    }
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }
}
