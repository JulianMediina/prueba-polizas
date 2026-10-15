# Red mínima para la demo: una subred pública, sin NAT Gateway (costo cero).
# La instancia solo acepta HTTP desde los nodos de borde de CloudFront (prefix list administrada por AWS);
# no hay puerto 22: la administración es por SSM Session Manager.

resource "aws_vpc" "this" {
  cidr_block           = var.cidr
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags                 = { Name = "${var.nombre}-vpc" }
}

# Neutraliza el security group por defecto (sin reglas)
resource "aws_default_security_group" "this" {
  vpc_id = aws_vpc.this.id
  tags   = { Name = "${var.nombre}-default-sin-reglas" }
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id
  tags   = { Name = "${var.nombre}-igw" }
}

resource "aws_subnet" "publica" {
  vpc_id                  = aws_vpc.this.id
  cidr_block              = cidrsubnet(var.cidr, 2, 0)
  availability_zone       = var.zona
  map_public_ip_on_launch = false
  tags                    = { Name = "${var.nombre}-publica-${var.zona}" }
}

resource "aws_route_table" "publica" {
  vpc_id = aws_vpc.this.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }
  tags = { Name = "${var.nombre}-rt-publica" }
}

resource "aws_route_table_association" "publica" {
  subnet_id      = aws_subnet.publica.id
  route_table_id = aws_route_table.publica.id
}

data "aws_ec2_managed_prefix_list" "cloudfront" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

resource "aws_security_group" "app" {
  name        = "${var.nombre}-app"
  description = "Origen de CloudFront: HTTP solo desde la prefix list de CloudFront"
  vpc_id      = aws_vpc.this.id
  tags        = { Name = "${var.nombre}-app" }
}

resource "aws_vpc_security_group_ingress_rule" "http_cloudfront" {
  security_group_id = aws_security_group.app.id
  description       = "HTTP desde CloudFront"
  prefix_list_id    = data.aws_ec2_managed_prefix_list.cloudfront.id
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
}

# Salida necesaria para: imágenes de ECR, paquetes del SO y endpoints de SSM
resource "aws_vpc_security_group_egress_rule" "https" {
  security_group_id = aws_security_group.app.id
  description       = "HTTPS saliente (ECR, SSM, repositorios del SO)"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
}

resource "aws_vpc_security_group_egress_rule" "http" {
  security_group_id = aws_security_group.app.id
  description       = "HTTP saliente (repositorios del SO)"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
}
